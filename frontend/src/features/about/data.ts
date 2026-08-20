import type { Principle, Stat } from "./types";

export const principles: Principle[] = [
  { id: "p1", order: 1, title: "누구나 쉽게", description: "어려운 법률 용어 대신 평소 쓰는 말로 질문하고 이해할 수 있어요." },
  { id: "p2", order: 2, title: "정확한 법률 근거", description: "모든 답변에 관련 법 조문과 판례를 함께 제시합니다." },
  { id: "p3", order: 3, title: "개인정보 보호", description: "상담 중 입력된 개인정보는 자동으로 마스킹 처리됩니다." },
  { id: "p4", order: 4, title: "24시간 이용 가능", description: "로그인 없이도 언제든 바로 상담을 시작할 수 있어요." },
];

export const stats: Stat[] = [
  { id: "s1", value: "128,000+", label: "누적 상담 건수" },
  { id: "s2", value: "42,000+", label: "이용자 수" },
  { id: "s3", value: "97%", label: "답변 정확도" },
  { id: "s4", value: "6초", label: "평균 응답 시간" },
];
