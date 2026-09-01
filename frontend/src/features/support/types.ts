export type SupportTab = "inquiry-form" | "inquiry-list" | "find-account";

export type InquiryCategory = "BUG" | "USAGE" | "BILLING" | "ACCOUNT" | "ETC";

export const INQUIRY_CATEGORY_LABELS: Record<InquiryCategory, string> = {
  BUG: "버그 제보",
  USAGE: "이용 문의",
  BILLING: "결제·요금",
  ACCOUNT: "계정",
  ETC: "기타",
};

export type InquiryStatus = "PENDING" | "ANSWERED";

export const INQUIRY_STATUS_LABELS: Record<InquiryStatus, string> = {
  PENDING: "답변대기",
  ANSWERED: "답변완료",
};

// GET /api/inquiries/me 목록 항목 — content/answerContent는 상세에서만 내려옴
export interface InquiryListItem {
  inquiryId: number;
  category: InquiryCategory;
  categoryLabel: string;
  title: string;
  status: InquiryStatus;
  statusLabel: string;
  createdAt: string;
  answeredAt: string | null;
}

// GET /api/inquiries/{id} 상세
export interface InquiryDetail extends InquiryListItem {
  content: string;
  screenshotUrl: string | null; // 절대 URL, 첨부 없으면 null
  answerContent: string | null;
}

export const formatInquiryDate = (isoDate: string): string => {
  const d = new Date(isoDate);
  const yyyy = d.getFullYear();
  const mm = String(d.getMonth() + 1).padStart(2, "0");
  const dd = String(d.getDate()).padStart(2, "0");
  return `${yyyy}.${mm}.${dd}`;
};