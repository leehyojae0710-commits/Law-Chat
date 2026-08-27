import { apiClient } from "./client";

// ===== 대시보드 =====
export interface DashboardStats {
  totalDislikes: number;
  weeklyDislikes: number;
  totalLikes: number;
  dislikeRate: number;
}

export const getDashboardStats = async (): Promise<DashboardStats> => {
  const res = await apiClient.get<DashboardStats>("/admin/dashboard/stats");
  return res.data;
};

// ===== 1:1 문의 처리 =====
export interface Inquiry {
  id: string;
  title: string;
  content: string;
  authorEmail: string;
  status: "미답변" | "답변완료";
  createdAt: string;
}

export const getInquiries = async (): Promise<Inquiry[]> => {
  const res = await apiClient.get<Inquiry[]>("/admin/inquiries");
  return res.data;
};

export const answerInquiry = async (inquiryId: string, answer: string): Promise<void> => {
  await apiClient.post(`/admin/inquiries/${inquiryId}/answer`, { answer });
};

// ===== 공지사항 관리 =====
import type { Notice, NoticeCategory, NoticeListItem, NoticePopup, NoticePopupAdmin, PageResponse } from "../features/notice/types";

// 백엔드에 admin 전용 목록 API가 없어서, 공개 목록 API를 재사용합니다.
export const getAdminNotices = async (page = 0, size = 50): Promise<PageResponse<NoticeListItem>> => {
  const res = await apiClient.get<PageResponse<NoticeListItem>>("/notices", { params: { page, size } });
  return res.data;
};

export const createNotice = async (notice: {
  category: NoticeCategory;
  title: string;
  content: string;
  fileUrl?: string;
}): Promise<number> => {
  const res = await apiClient.post<number>("/admin/notices", notice);
  return res.data;
};

export const updateNotice = async (
  noticeId: number,
  notice: Partial<Pick<Notice, "title" | "content" | "fileUrl">>
): Promise<void> => {
  await apiClient.patch(`/admin/notices/${noticeId}`, notice);
};

export const deleteNotice = async (noticeId: number): Promise<void> => {
  await apiClient.delete(`/admin/notices/${noticeId}`);
};

export const toggleNoticePin = async (noticeId: number): Promise<void> => {
  await apiClient.patch(`/admin/notices/${noticeId}/pin`);
};

// ===== 팝업 공지 관리 =====

export const getAdminPopups = async (): Promise<NoticePopupAdmin[]> => {
  const res = await apiClient.get<NoticePopupAdmin[]>("/admin/notices/popups");
  return res.data;
};

export const createPopup = async (popup: {
  title: string;
  fileUrl: string;
  linkUrl?: string;
  altText?: string;
  startDate: string; // ISO
  endDate: string;   // ISO
}): Promise<number> => {
  const res = await apiClient.post<number>("/admin/notices/popups", popup);
  return res.data;
};

export const updatePopup = async (
  popupId: number,
  popup: Partial<Omit<NoticePopup, "popupId">> & { startDate?: string; endDate?: string }
): Promise<void> => {
  await apiClient.patch(`/admin/notices/popups/${popupId}`, popup);
};

export const deletePopup = async (popupId: number): Promise<void> => {
  await apiClient.delete(`/admin/notices/popups/${popupId}`);
};

// ===== 파일 업로드 (공지/팝업 이미지·첨부용) =====

export const uploadNoticeFile = async (file: File): Promise<string> => {
  const formData = new FormData();
  formData.append("file", file);
  const res = await apiClient.post<{ fileUrl: string }>("/admin/notices/upload", formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });
  return res.data.fileUrl;
};