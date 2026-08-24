export type QuestionCategory =
  | "전체"
  | "계약/거래"
  | "이혼/가족"
  | "금전/채무"
  | "부동산"
  | "근로/직장";

export interface Question {
  id: string;
  category: QuestionCategory;
  text: string;
}

export interface ChatExample {
  question: string;
  answerTitle: string;
  bullets: string[];
}