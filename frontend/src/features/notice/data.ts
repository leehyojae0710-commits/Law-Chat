import type { Notice } from "./types";

export const noticeCategories = ["전체", "서비스 업데이트", "이벤트", "점검 안내", "이용약관", "채용"];

export const notices: Notice[] = [
  { id: "n1", category: "이용약관", title: "[필독] 개인정보처리방침 및 이용약관 개정 안내", date: "2026.08.10", pinned: true },
  { id: "n2", category: "서비스 업데이트", title: "AI 답변에 판례 원문 링크가 함께 제공됩니다", date: "2026.08.05" },
  { id: "n3", category: "점검 안내", title: "8월 14일(금) 새벽 서버 정기 점검 예정", date: "2026.08.09" },
];
