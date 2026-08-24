import { useEffect, useState } from "react";
import type { Notice } from "../types";
import { mockPopupNotices } from "../data";
import { getPopupNotices } from "../../../api/notice";

const USE_MOCK = import.meta.env.VITE_USE_MOCK_NOTICE === "true";

export const useActivePopupNotices = () => {
  const [notices, setNotices] = useState<Notice[]>([]);

  useEffect(() => {
    const dismissed = getDismissedIds();

    if (USE_MOCK) {
      const today = new Date();
      const filtered = mockPopupNotices.filter((n) => {
        if (!n.popupStartDate || !n.popupEndDate) return false;
        const start = new Date(n.popupStartDate);
        const end = new Date(n.popupEndDate);
        return today >= start && today <= end;
      });
      setNotices(filtered.filter((n) => !dismissed.includes(n.id)));
      return;
    }

    getPopupNotices()
      .then((data) => setNotices(data.filter((n) => !dismissed.includes(n.id))))
      .catch(() => setNotices([]));
  }, []);

  return notices;
};

const DISMISS_KEY = "dismissedNoticeIds";

const getDismissedIds = (): string[] => {
  try {
    const raw = localStorage.getItem(DISMISS_KEY);
    if (!raw) return [];
    const parsed: { id: string; until: number }[] = JSON.parse(raw);
    const now = Date.now();
    return parsed.filter((d) => d.until > now).map((d) => d.id);
  } catch {
    return [];
  }
};

export const dismissNoticeForDays = (id: string, days: number) => {
  const raw = localStorage.getItem(DISMISS_KEY);
  const list: { id: string; until: number }[] = raw ? JSON.parse(raw) : [];
  const until = Date.now() + days * 24 * 60 * 60 * 1000;
  localStorage.setItem(
    DISMISS_KEY,
    JSON.stringify([...list.filter((d) => d.id !== id), { id, until }])
  );
};