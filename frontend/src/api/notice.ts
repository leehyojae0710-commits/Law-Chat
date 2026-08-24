import { apiClient } from "./client";
import type { Notice } from "../features/notice/types";

// 백엔드가 오늘 날짜 기준 popupStartDate ~ popupEndDate 사이인 공지만 필터해서 내려줍니다.
export const getPopupNotices = async (): Promise<Notice[]> => {
  const res = await apiClient.get<Notice[]>("/notice/popup");
  return res.data;
};