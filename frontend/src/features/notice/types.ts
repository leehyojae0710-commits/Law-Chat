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
  // 공지 상세 응답에 포함되는 팝업 복원용 필드 (팝업이 없으면 hasPopup: false, 나머지는 없음)
  hasPopup?: boolean;
  popupStartDate?: string;
  popupEndDate?: string;
}

export type NoticeListItem = Omit<Notice, "content" | "fileUrl" | "updatedAt">;

export interface NoticePopup {
  popupId: number;
  noticeId: number | null; // 이 팝업이 어느 공지에서 만들어졌는지 (독립 생성된 팝업이면 null)
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
  // 공지 삭제 시 이 팝업도 함께 삭제되는지 여부 (표시용 배지)
  linkedToNotice: boolean;
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