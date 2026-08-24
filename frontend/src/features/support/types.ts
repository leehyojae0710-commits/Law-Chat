export type SupportTab = "inquiry-form" | "inquiry-list" | "find-account";

export interface Inquiry {
  id: string;
  title: string;
  status: "답변대기" | "답변완료";
  date: string;
}
