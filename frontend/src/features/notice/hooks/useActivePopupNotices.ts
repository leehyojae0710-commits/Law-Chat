import { useEffect, useState } from "react";
import type { Notice } from "../types";
import { mockPopupNotices } from "../data";
import { getPopupNotices } from "../../../api/notice";

const USE_MOCK = import.meta.env.VITE_USE_MOCK_NOTICE === "true";
const DISMISS_KEY = "popupDismissedUntil";

export const useActivePopupNotices = () => {
  const [notices, setNotices] = useState<Notice[]>([]);

  useEffect(() => {
    if (isPopupDismissed()) {
      setNotices([]);
      return;
    }

    if (USE_MOCK) {
      const today = new Date();
      const filtered = mockPopupNotices.filter((n) => {
        if (!n.popupStartDate || !n.popupEndDate) return false;
        const start = new Date(n.popupStartDate);
        const end = new Date(n.popupEndDate);
        return today >= start && today <= end;
      });
      setNotices(filtered);
      return;
    }

    getPopupNotices()
      .then(setNotices)
      .catch(() => setNotices([]));
  }, []);

  return notices;
};

const isPopupDismissed = (): boolean => {
  const until = localStorage.getItem(DISMISS_KEY);
  if (!until) return false;
  return Date.now() < Number(until);
};

export const dismissPopupForDays = (days: number) => {
  const until = Date.now() + days * 24 * 60 * 60 * 1000;
  localStorage.setItem(DISMISS_KEY, String(until));
};