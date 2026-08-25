import { useParams } from "react-router-dom";
import { useEffect, useRef } from "react";
import { useChat } from "../features/chat/hooks/useChat";
import { MessageBubble } from "../features/chat/components/MessageBubble";
import { NewChatInput } from "../features/chat/components/NewChatInput";

export const ChatPage = () => {
  const { conversationId } = useParams();
  const { messages, isLoadingHistory, isSending, error, sendMessage, giveFeedback } = useChat(conversationId);
  const bottomRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages.length, isSending]);

  const hasMessages = messages.length > 0;

  if (isLoadingHistory) {
    return <div className="flex h-full items-center justify-center text-sm text-slate-400">불러오는 중이에요…</div>;
  }

  // 메시지가 없는 상태 = 새 상담 시작 화면 (히어로 입력창 + 예시 질문)
  if (!hasMessages) {
    return (
      <div className="mx-auto flex h-full max-w-3xl flex-col justify-center px-6">
        <NewChatInput onSend={sendMessage} isSending={isSending} variant="hero" />
        {error && <p className="mt-3 text-sm text-red-500">{error}</p>}
      </div>
    );
  }

  // 메시지가 있는 상태 = 이어서 상담 화면 (스레드 + 하단 고정 입력창)
  return (
    <div className="flex h-full flex-col">
      <div className="mx-auto w-full max-w-3xl flex-1 space-y-6 overflow-y-auto px-6 py-8">
        {messages.map((m) => (
          <MessageBubble
            key={m.id}
            message={m}
            onFeedback={(type, reasonCode, detail) => giveFeedback(m.id, type, reasonCode, detail)}
          />
        ))}
        {isSending && (
          <div className="flex items-center gap-2 pl-11 text-sm text-slate-400">
            <span className="h-1.5 w-1.5 animate-bounce rounded-full bg-slate-300" style={{ animationDelay: "0ms" }} />
            <span className="h-1.5 w-1.5 animate-bounce rounded-full bg-slate-300" style={{ animationDelay: "120ms" }} />
            <span className="h-1.5 w-1.5 animate-bounce rounded-full bg-slate-300" style={{ animationDelay: "240ms" }} />
            <span className="ml-1">답변을 작성하고 있어요…</span>
          </div>
        )}
        {error && <p className="pl-11 text-sm text-red-500">{error}</p>}
        <div ref={bottomRef} />
      </div>

      <div className="mx-auto w-full max-w-3xl">
        <NewChatInput onSend={sendMessage} isSending={isSending} variant="compact" />
      </div>
    </div>
  );
};
