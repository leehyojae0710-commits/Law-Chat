import { apiClient } from "./client";
import type { InquiryCategory, InquiryStatus } from "../features/support/types";
import type { Notice, NoticeCategory, NoticeListItem, NoticePopup, NoticePopupAdmin, PageResponse } from "../features/notice/types";

// ===== 대시보드 =====
// 좋아요는 집계하지 않음. "신고(싫어요) 건수 + 비율 + 사유 카테고리별 분포 + 최근 피드백"이 응답으로 온다.
// (features/chat/types.ts의 FeedbackReasonCode와 code 값이 1:1로 대응됨)
export interface DashboardReasonStat {
  code: string;
  label: string;
  count: number;
  percent: number; // 0~100, 소수 첫째자리
}

export interface DashboardRecentFeedbackItem {
  feedbackId: number;
  title: string; // 질문 원문을 짧게 자른 것 (실제 제목 컬럼은 없음)
  reasonCode: string;
  reasonLabel: string;
  reasonDetail: string; // 상세설명이 없으면 reasonLabel과 동일한 값이 옴
  createdAt: string;
}

export interface DashboardStats {
  totalFeedbackCount: number;
  weeklyFeedbackCount: number;
  dislikeRatioPercent: number; // 0~100, 소수 첫째자리
  reasonBreakdown: DashboardReasonStat[];
  recentFeedback: DashboardRecentFeedbackItem[];
  updatedAt: string; // 이 응답이 만들어진 시각 (ISO)
}

export const getDashboardStats = async (): Promise<DashboardStats> => {
  const res = await apiClient.get<DashboardStats>("/admin/dashboard/stats");
  return res.data;
};

// ===== 1:1 문의 처리 =====
// 관리자 목록/상세 응답 - 사용자용과 달리 content, answerContent(미승인 포함), 작성자 정보까지 항상 포함됨
export interface AdminInquiryItem {
  inquiryId: number;
  category: InquiryCategory;
  categoryLabel: string;
  title: string;
  content: string;
  screenshotUrl: string | null; // 절대 URL, 첨부 없으면 null
  // 탈퇴 회원이면 셋 다 null, 익명화된 회원이면 authorEmail만 null
  authorId: number | null;
  authorEmail: string | null;
  authorNickname: string | null;
  status: InquiryStatus;
  statusLabel: string;
  answerContent: string | null;
  answeredAt: string | null;
  createdAt: string;
}

export const getAdminInquiries = async (
  status?: InquiryStatus,
  category?: InquiryCategory,
  page = 0,
  size = 20
): Promise<PageResponse<AdminInquiryItem>> => {
  const res = await apiClient.get<PageResponse<AdminInquiryItem>>("/admin/inquiries", {
    params: { status, category, page, size },
  });
  return res.data;
};

export const getAdminInquiry = async (inquiryId: number): Promise<AdminInquiryItem> => {
  const res = await apiClient.get<AdminInquiryItem>(`/admin/inquiries/${inquiryId}`);
  return res.data;
};

// 같은 엔드포인트로 등록/수정 둘 다 처리됨 (이미 답변이 있으면 덮어쓰고 answeredAt 갱신)
export const answerInquiry = async (inquiryId: number, answerContent: string): Promise<void> => {
  await apiClient.post(`/admin/inquiries/${inquiryId}/answer`, { answerContent });
};

// ===== 공지사항 관리 =====

export const getAdminNotices = async (page = 0, size = 10): Promise<PageResponse<NoticeListItem>> => {
  const res = await apiClient.get<PageResponse<NoticeListItem>>("/admin/notices", { params: { page, size } });
  return res.data;
};

export const createNotice = async (notice: {
  category: NoticeCategory;
  title: string;
  content: string;
  fileUrl?: string;
  createPopup: boolean;
  popupStartDate?: string; // createPopup이 true일 때만 필요, ISO 문자열
  popupEndDate?: string;   // createPopup이 true일 때만 필요, ISO 문자열
}): Promise<number> => {
  const res = await apiClient.post<number>("/admin/notices", notice);
  return res.data;
};

export const updateNotice = async (
  noticeId: number,
  notice: Partial<Pick<Notice, "title" | "content" | "fileUrl">> & {
    createPopup: boolean;
    popupStartDate?: string;
    popupEndDate?: string;
  }
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
  noticeId?: number; // 이 팝업이 어떤 공지에서 만들어졌는지 백엔드에 같이 저장
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