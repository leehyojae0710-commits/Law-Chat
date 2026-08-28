export type NoticeCategory = "GENERAL" | "SYSTEM" | "EVENT";

export const NOTICE_CATEGORY_LABELS: Record<NoticeCategory, string> = {
  GENERAL: "일반",
  SYSTEM: "점검 안내",
  EVENT: "이벤트",
};

export interface Notice {
  noticeId: number;
  category: NoticeCategory;
  title: string;
  content: string;
  fileUrl?: string;
  isPinned: boolean;
  createdAt: string;
  updatedAt?: string;
}

export type NoticeListItem = Omit<Notice, "content" | "fileUrl" | "updatedAt">;

export interface NoticePopup {
  popupId: number;
  title: string;
  fileUrl: string;
  altText?: string;
}

/** 관리자 화면용 - 노출 기간, 활성 여부까지 포함 (백엔드 NoticePopupAdminResponse) */
export interface NoticePopupAdmin extends NoticePopup {
  startDate: string;
  endDate: string;
  createdAt: string;
  isActive: boolean;
}

/** Spring Data Page<T> 응답 형태 그대로 */
export interface PageResponse<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  number: number; // 현재 페이지 (0-base)
  size: number;
  first: boolean;
  last: boolean;
}

export const formatNoticeDate = (isoDate: string): string => {
  const d = new Date(isoDate);
  const yyyy = d.getFullYear();
  const mm = String(d.getMonth() + 1).padStart(2, "0");
  const dd = String(d.getDate()).padStart(2, "0");
  return `${yyyy}.${mm}.${dd}`;
};