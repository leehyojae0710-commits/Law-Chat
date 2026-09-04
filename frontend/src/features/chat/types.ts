export interface LegalSource {
  lawName: string;
  articleNumber: string;
  caseNum?: string;
  url: string;
}

export type FeedbackType = "like" | "dislike";

export type FeedbackReasonCode =
  | "TERM_MISMATCH" // 법률 용어 변환이 정확하지 않음
  | "WRONG_CATEGORY" // 법률 분야가 잘못 분류됨
  | "WRONG_SOURCE" // 근거 조문이나 판례가 사실과 다름
  | "OFF_INTENT" // 질문 의도와 다른 답변
  | "OTHER"; // 기타

export interface FeedbackReasonOption {
  code: FeedbackReasonCode;
  label: string;
}

export interface MessageFeedback {
  type: FeedbackType;
  reasonCode?: FeedbackReasonCode;
  detail?: string;
}

export interface ChatMessage {
  id: string;
  role: "user" | "assistant";
  content: string;
  sources?: LegalSource[];
  createdAt: string;
  feedback?: MessageFeedback | null;
}

export type ConversationCategory =
  | "계약/거래"
  | "이혼/가족"
  | "금전/채무"
  | "부동산"
  | "근로/직장"
  | "기타";

export interface Conversation {
  id: string;
  title: string;
  preview: string;
  category: ConversationCategory;
  updatedAt: string;
  hasSummary: boolean;
  isFavorite: boolean;
}

export interface ConversationDetail extends Conversation {
  messages: ChatMessage[];
}

export interface SendMessageResult {
  conversation: Conversation;
  userMessage: ChatMessage;
  assistantMessage: ChatMessage;
}
