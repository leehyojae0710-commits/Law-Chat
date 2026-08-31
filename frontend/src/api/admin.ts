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


export const getAdminNotices = async (page = 0, size = 10): Promise<PageResponse<NoticeListItem>> => {
  const res = await apiClient.get<PageResponse<NoticeListItem>>("/admin/notices", { params: { page, size } });
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

export interface UploadedNoticeFile {
  fileName: string; // 공지/팝업 등록 요청의 fileUrl 필드에 그대로 넣을 값
  fileUrl: string;  // 업로드 직후 미리보기(<img src>)에만 사용
}

export const uploadNoticeFile = async (file: File): Promise<UploadedNoticeFile> => {
  const formData = new FormData();
  formData.append("file", file);
  // Content-Type을 수동으로 지정하지 않음 — axios/브라우저가 FormData를 보고
  // boundary가 포함된 정확한 multipart Content-Type을 자동으로 설정하도록 둠.
  const res = await apiClient.post<UploadedNoticeFile>("/admin/notices/upload", formData);
  return res.data;
};