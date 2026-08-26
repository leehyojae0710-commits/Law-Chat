// 백엔드가 완성되기 전, 프론트만 테스트하기 위한 mock 채팅 로직입니다.
// 실제 API 연동이 끝나면 useChat.ts / useConversations.ts에서
// .env의 VITE_USE_MOCK_CHAT=false 로만 바꾸면 되고, 이 파일은 더 이상 호출되지 않습니다.
import type {
  ChatMessage,
  Conversation,
  ConversationDetail,
  FeedbackReasonCode,
  FeedbackType,
  SendMessageResult,
} from "./types";
import { matchAnswerRule, mockConversationMessages, mockConversations } from "./data";

// 새로고침하면 초기화되는 메모리 저장소입니다.
const runtimeConversations: Conversation[] = mockConversations.map((c) => ({ ...c }));
const runtimeMessages: Record<string, ChatMessage[]> = Object.fromEntries(
  Object.entries(mockConversationMessages).map(([id, msgs]) => [id, msgs.map((m) => ({ ...m }))])
);

const fakeDelay = (ms = 500) => new Promise((resolve) => setTimeout(resolve, ms));
let idCounter = 1000;
const nextId = (prefix: string) => `${prefix}-${idCounter++}`;

const buildTitle = (content: string) =>
  content.length > 20 ? `${content.slice(0, 20)}…` : content;

const buildAssistantMessage = (question: string): ChatMessage => {
  const rule = matchAnswerRule(question);
  return {
    id: nextId("msg"),
    role: "assistant",
    content: rule.content,
    sources: rule.sources,
    createdAt: new Date().toISOString(),
  };
};

export const mockListConversations = async (params?: {
  favorite?: boolean;
  keyword?: string;
}): Promise<Conversation[]> => {
  await fakeDelay(300);
  let list = [...runtimeConversations];
  if (params?.favorite) list = list.filter((c) => c.isFavorite);
  if (params?.keyword) {
    const kw = params.keyword.trim();
    if (kw) {
      list = list.filter((c) => c.title.includes(kw) || c.preview.includes(kw));
    }
  }
  return list.sort((a, b) => (a.updatedAt < b.updatedAt ? 1 : -1));
};

export const mockGetConversation = async (id: string): Promise<ConversationDetail> => {
  await fakeDelay(300);
  const conversation = runtimeConversations.find((c) => c.id === id);
  if (!conversation) throw new Error("존재하지 않는 상담이에요");
  return { ...conversation, messages: runtimeMessages[id] ?? [] };
};

export const mockStartConversation = async (content: string): Promise<SendMessageResult> => {
  await fakeDelay(700);
  const rule = matchAnswerRule(content);
  const id = nextId("conv");
  const userMessage: ChatMessage = {
    id: nextId("msg"),
    role: "user",
    content,
    createdAt: new Date().toISOString(),
  };
  const assistantMessage = buildAssistantMessage(content);

  const conversation: Conversation = {
    id,
    title: buildTitle(content) || rule.title,
    preview: rule.content.replace(/\*\*/g, "").split("\n")[0].slice(0, 60),
    category: rule.category,
    updatedAt: new Date().toISOString(),
    hasSummary: false,
    isFavorite: false,
  };

  runtimeConversations.unshift(conversation);
  runtimeMessages[id] = [userMessage, assistantMessage];

  return { conversation, userMessage, assistantMessage };
};

export const mockSendMessage = async (
  conversationId: string,
  content: string
): Promise<{ userMessage: ChatMessage; assistantMessage: ChatMessage }> => {
  await fakeDelay(700);
  const conversation = runtimeConversations.find((c) => c.id === conversationId);
  if (!conversation) throw new Error("존재하지 않는 상담이에요");

  const userMessage: ChatMessage = {
    id: nextId("msg"),
    role: "user",
    content,
    createdAt: new Date().toISOString(),
  };
  const assistantMessage = buildAssistantMessage(content);

  runtimeMessages[conversationId] = [...(runtimeMessages[conversationId] ?? []), userMessage, assistantMessage];
  conversation.updatedAt = new Date().toISOString();

  return { userMessage, assistantMessage };
};

export const mockToggleFavoriteConversation = async (
  id: string,
  isFavorite: boolean
): Promise<Conversation> => {
  await fakeDelay(150);
  const conversation = runtimeConversations.find((c) => c.id === id);
  if (!conversation) throw new Error("존재하지 않는 상담이에요");
  conversation.isFavorite = isFavorite;
  return conversation;
};

export const mockDeleteConversation = async (id: string): Promise<void> => {
  await fakeDelay(150);
  const idx = runtimeConversations.findIndex((c) => c.id === id);
  if (idx >= 0) runtimeConversations.splice(idx, 1);
  delete runtimeMessages[id];
};

export const mockSubmitMessageFeedback = async (
  messageId: string,
  payload: { type: FeedbackType; reasonCode?: FeedbackReasonCode; detail?: string }
): Promise<void> => {
  await fakeDelay(200);
  for (const messages of Object.values(runtimeMessages)) {
    const target = messages.find((m) => m.id === messageId);
    if (target) {
      target.feedback = { type: payload.type, reasonCode: payload.reasonCode, detail: payload.detail };
      break;
    }
  }
};
