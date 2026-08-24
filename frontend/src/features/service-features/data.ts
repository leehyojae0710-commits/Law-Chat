import type { ServiceFeature } from "./types";

export const serviceFeatures: ServiceFeature[] = [
  { id: "f1", order: 1, title: "AI 법률 상담", description: "일상어로 질문하면 법률 쟁점을 정리해 답변합니다." },
  { id: "f2", order: 2, title: "근거 조문·판례 제시", description: "모든 답변에 관련 법 조문과 판례를 함께 안내합니다." },
  { id: "f3", order: 3, title: "상담 히스토리", description: "이전 상담 내용을 언제든 다시 확인할 수 있어요." },
  { id: "f4", order: 4, title: "요약서 PDF 다운로드", description: "상담 내용을 정리한 요약서를 PDF로 저장합니다." },
  { id: "f5", order: 5, title: "즐겨찾기", description: "중요한 상담을 즐겨찾기에 담아 빠르게 찾아보세요." },
  { id: "f6", order: 6, title: "개인정보 마스킹", description: "이름·연락처 등 민감 정보는 자동으로 가려집니다." },
];
