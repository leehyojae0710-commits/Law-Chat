import { apiClient } from "./client";
import type { Notice, NoticeCategory, NoticeListItem, NoticePopup, PageResponse } from "../features/notice/types";

export const getNotices = async (
  category?: NoticeCategory,
  page = 0,
  size = 10
): Promise<PageResponse<NoticeListItem>> => {
  const res = await apiClient.get<PageResponse<NoticeListItem>>("/notices", {
    params: { category, page, size },
  });
  console.log(res.data);
  return res.data;
};

export const getNotice = async (noticeId: number): Promise<Notice> => {
  const res = await apiClient.get<Notice>(`/notices/${noticeId}`);
  console.log(res.data);
  return res.data;
};

// 백엔드가 오늘 날짜 기준 startDate ~ endDate 사이인 팝업만 필터해서 내려줍니다.
export const getPopupNotices = async (): Promise<NoticePopup[]> => {
  const res = await apiClient.get<NoticePopup[]>("/notices/popups/active");
  return res.data;
};