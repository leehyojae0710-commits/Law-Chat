import { useCallback, useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import type { ChatMessage, FeedbackReasonCode, FeedbackType } from "../types";
import {
  getConversation as getConversationApi,
  sendMessage as sendMessageApi,
  startConversation as startConversationApi,
  submitMessageFeedback as submitMessageFeedbackApi,
} from "../../../api/chat";
import {
  mockGetConversation,
  mockSendMessage,
  mockStartConversation,
  mockSubmitMessageFeedback,
} from "../mockChat";

// 백엔드가 준비되기 전에는 .env의 VITE_USE_MOCK_CHAT=true 로 mock 데이터를 사용합니다.
// 백엔드 연동 시 .env에서 VITE_USE_MOCK_CHAT=false 로만 바꾸면 실제 API를 호출합니다.
const USE_MOCK_CHAT = import.meta.env.VITE_USE_MOCK_CHAT === "true";

export const useChat = (conversationId?: string) => {
  const navigate = useNavigate();
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [title, setTitle] = useState<string | null>(null);
  const [isLoadingHistory, setIsLoadingHistory] = useState(false);
  const [isSending, setIsSending] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const activeConversationId = useRef<string | undefined>(conversationId);

  useEffect(() => {
    activeConversationId.current = conversationId;

    if (!conversationId) {
      setMessages([]);
      setTitle(null);
      return;
    }

    setIsLoadingHistory(true);
    setError(null);
    const loader = USE_MOCK_CHAT ? mockGetConversation(conversationId) : getConversationApi(conversationId);
    loader
      .then((detail) => {
        setMessages(detail.messages);
        setTitle(detail.title);
      })
      .catch(() => setError("상담 내용을 불러오지 못했어요."))
      .finally(() => setIsLoadingHistory(false));
  }, [conversationId]);

  const sendMessage = useCallback(
    async (content: string) => {
      const text = content.trim();
      if (!text || isSending) return;

      setError(null);
      setIsSending(true);

      // 낙관적 업데이트: 사용자 메시지를 먼저 화면에 반영합니다.
      const optimisticUserMessage: ChatMessage = {
        id: `pending-${Date.now()}`,
        role: "user",
        content: text,
        createdAt: new Date().toISOString(),
      };
      setMessages((prev) => [...prev, optimisticUserMessage]);

      try {
        if (!activeConversationId.current) {
          const result = USE_MOCK_CHAT
            ? await mockStartConversation(text)
            : await startConversationApi(text);
          activeConversationId.current = result.conversation.id;
          setMessages([result.userMessage, result.assistantMessage]);
          setTitle(result.conversation.title);
          // 새 상담이 생성되면 URL을 상담 id로 교체합니다 (히스토리에서 이어서 상담할 수 있도록).
          navigate(`/chat/${result.conversation.id}`, { replace: true });
        } else {
          const result = USE_MOCK_CHAT
            ? await mockSendMessage(activeConversationId.current, text)
            : await sendMessageApi(activeConversationId.current, text);
          setMessages((prev) => [
            ...prev.filter((m) => m.id !== optimisticUserMessage.id),
            result.userMessage,
            result.assistantMessage,
          ]);
        }
      } catch {
        setError("답변을 가져오지 못했어요. 잠시 후 다시 시도해주세요.");
        setMessages((prev) => prev.filter((m) => m.id !== optimisticUserMessage.id));
      } finally {
        setIsSending(false);
      }
    },
    [isSending, navigate]
  );

  const giveFeedback = useCallback(
    async (messageId: string, type: FeedbackType, reasonCode?: FeedbackReasonCode, detail?: string) => {
      setMessages((prev) =>
        prev.map((m) => (m.id === messageId ? { ...m, feedback: { type, reasonCode, detail } } : m))
      );
      try {
        if (USE_MOCK_CHAT) {
          await mockSubmitMessageFeedback(messageId, { type, reasonCode, detail });
        } else {
          await submitMessageFeedbackApi(messageId, { type, reasonCode, detail });
        }
      } catch {
        // 피드백 전송 실패는 조용히 무시하고 UI 상태는 유지합니다.
      }
    },
    []
  );

  return {
    messages,
    title,
    isLoadingHistory,
    isSending,
    error,
    sendMessage,
    giveFeedback,
  };
};
