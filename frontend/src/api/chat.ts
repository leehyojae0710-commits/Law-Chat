import { apiClient } from "./client";
import type {
  ChatMessage,
  Conversation,
  ConversationDetail,
  FeedbackType,
  FeedbackReasonCode,
  SendMessageResult,
} from "../features/chat/types";

// 백엔드 API 계약 (2026.08.11 화면설계서 v7 기준)
// GET    /chat/conversations              -> 상담 히스토리 목록
// GET    /chat/conversations?favorite=1   -> 즐겨찾기한 상담 목록
// GET    /chat/conversations/:id          -> 특정 상담 상세(메시지 포함)
// POST   /chat/conversations              -> 새 상담 시작 (첫 메시지 전송)
// POST   /chat/conversations/:id/messages -> 기존 상담에 이어서 메시지 전송
// PATCH  /chat/conversations/:id/favorite -> 즐겨찾기 토글
// DELETE /chat/conversations/:id          -> 상담 삭제
// POST   /chat/messages/:id/feedback      -> 답변 좋아요 · 싫어요 피드백

export const listConversations = async (params?: {
  favorite?: boolean;
  keyword?: string;
}): Promise<Conversation[]> => {
  const res = await apiClient.get<Conversation[]>("/chat/conversations", {
    params: {
      favorite: params?.favorite ? 1 : undefined,
      keyword: params?.keyword || undefined,
    },
  });
  return res.data;
};

export const getConversation = async (id: string): Promise<ConversationDetail> => {
  const res = await apiClient.get<ConversationDetail>(`/chat/conversations/${id}`);
  return res.data;
};

export const startConversation = async (content: string): Promise<SendMessageResult> => {
  const res = await apiClient.post<SendMessageResult>("/chat/conversations", { content });
  return res.data;
};

export const sendMessage = async (
  conversationId: string,
  content: string
): Promise<{ userMessage: ChatMessage; assistantMessage: ChatMessage }> => {
  const res = await apiClient.post<{ userMessage: ChatMessage; assistantMessage: ChatMessage }>(
    `/chat/conversations/${conversationId}/messages`,
    { content }
  );
  return res.data;
};

export const toggleFavoriteConversation = async (
  id: string,
  isFavorite: boolean
): Promise<Conversation> => {
  const res = await apiClient.patch<Conversation>(`/chat/conversations/${id}/favorite`, {
    isFavorite,
  });
  return res.data;
};

export const deleteConversation = async (id: string): Promise<void> => {
  await apiClient.delete(`/chat/conversations/${id}`);
};

export const submitMessageFeedback = async (
  messageId: string,
  payload: { type: FeedbackType; reasonCode?: FeedbackReasonCode; detail?: string }
): Promise<void> => {
  await apiClient.post(`/chat/messages/${messageId}/feedback`, payload);
};
