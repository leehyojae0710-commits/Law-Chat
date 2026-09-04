import { useState } from "react";
import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import type { ChatMessage, FeedbackReasonCode } from "../types";
import { FeedbackModal } from "./FeedbackModal";

interface MessageBubbleProps {
  message: ChatMessage;
  onFeedback: (type: "like" | "dislike", reasonCode?: FeedbackReasonCode, detail?: string) => void;
}

export const MessageBubble = ({ message, onFeedback }: MessageBubbleProps) => {
  const [showFeedbackModal, setShowFeedbackModal] = useState(false);
  const isUser = message.role === "user";

  if (isUser) {
    return (
      <div className="flex justify-end">
        <div className="max-w-[80%] whitespace-pre-wrap rounded-2xl rounded-tr-sm bg-violet-600 px-5 py-3.5 text-[15px] leading-relaxed text-white">
          {message.content}
        </div>
      </div>
    );
  }

  const liked = message.feedback?.type === "like";
  const disliked = message.feedback?.type === "dislike";

  return (
    <div className="flex items-start gap-3">
      <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-violet-600 text-xs font-bold text-white">
        L
      </div>

      <div className="max-w-[80%] space-y-3">
        <div className="rounded-2xl rounded-tl-sm bg-slate-100 px-5 py-3.5 text-[15px] leading-relaxed text-slate-800 dark:bg-slate-800 dark:text-slate-100">
          <div className="prose prose-sm prose-slate max-w-none prose-p:my-1.5 prose-strong:text-slate-900 prose-ul:my-1.5 dark:prose-invert dark:prose-strong:text-slate-100">
            <ReactMarkdown remarkPlugins={[remarkGfm]}>{message.content}</ReactMarkdown>
          </div>

          {message.sources && message.sources.length > 0 && (
            <div className="mt-3 flex flex-wrap gap-1.5 border-t border-slate-200 pt-3 dark:border-slate-700">
              {message.sources.map((s) => {
                // 법령 조문이면 "법령명 제n조", 판례면 사건번호를 라벨로 사용.
                // (caseNum을 안 쓰면 판례 항목은 lawName/articleNumber가 비어있어
                //  텍스트 없는 빈 버튼으로 보이는 문제가 있었음)
                const label = s.lawName || s.articleNumber
                  ? `${s.lawName} ${s.articleNumber}`.trim()
                  : s.caseNum || "출처 보기";
                return (
                  <a
                    key={`${s.lawName}-${s.articleNumber}-${s.caseNum}`}
                    href={s.url}
                    target="_blank"
                    rel="noreferrer"
                    className="rounded-md border border-violet-200 bg-white px-2.5 py-1 text-xs font-medium text-violet-700 hover:bg-violet-50 dark:border-violet-800 dark:bg-slate-800 dark:text-violet-300 dark:hover:bg-violet-900/30"
                  >
                    {label}
                  </a>
                );
              })}
            </div>
          )}
        </div>

        <div className="flex items-center gap-2 px-1 text-slate-400 dark:text-slate-500">
          <button
            type="button"
            onClick={() => onFeedback("like")}
            aria-pressed={liked}
            className={`rounded-md px-1.5 py-1 text-sm transition-colors hover:bg-slate-100 dark:hover:bg-slate-800 ${
              liked ? "text-violet-600" : ""
            }`}
          >
            👍
          </button>
          <button
            type="button"
            onClick={() => setShowFeedbackModal(true)}
            aria-pressed={disliked}
            className={`rounded-md px-1.5 py-1 text-sm transition-colors hover:bg-slate-100 dark:hover:bg-slate-800 ${
              disliked ? "text-red-500" : ""
            }`}
          >
            👎
          </button>
          {message.feedback && (
            <span className="text-xs text-slate-400 dark:text-slate-500">피드백을 보내주셔서 감사해요</span>
          )}
        </div>
      </div>

      {showFeedbackModal && (
        <FeedbackModal
          onClose={() => setShowFeedbackModal(false)}
          onSubmit={(reasonCode, detail) => {
            onFeedback("dislike", reasonCode, detail);
            setShowFeedbackModal(false);
          }}
        />
      )}
    </div>
  );
};