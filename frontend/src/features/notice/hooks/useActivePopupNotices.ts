import { useEffect, useState } from "react";
import type { NoticePopup } from "../types";
import { mockPopupNotices } from "../data";
import { getPopupNotices } from "../../../api/notice";

const USE_MOCK = import.meta.env.VITE_USE_MOCK_NOTICE === "true";
const DISMISS_KEY = "popupDismissedUntil";
const API_BASE_URL = import.meta.env.VITE_API_URL;

export const useActivePopupNotices = () => {
  const [notices, setNotices] = useState<NoticePopup[]>([]);

  useEffect(() => {
    if (isPopupDismissed()) {
      setNotices([]);
      return;
    }

    if (USE_MOCK) {
      setNotices(mockPopupNotices);
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

export const resolveFileUrl = (fileName: string): string => {
  if (!fileName) return "";

  // fileName에 이미 완성된 URL이 통째로 들어있는 경우, 마지막 파일명만 추출
  const cleanFileName = fileName.includes("/")
    ? fileName.split("/").pop()!
    : fileName;

  return `${API_BASE_URL}/files/download/2차/images/${cleanFileName}`;
};
