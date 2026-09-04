import { useState } from "react";
import type { KeyboardEvent } from "react";
import { exampleQuestions } from "../data";

const MAX_LENGTH = 2000;

interface NewChatInputProps {
  onSend: (content: string) => void;
  isSending: boolean;
  variant?: "hero" | "compact";
}

export const NewChatInput = ({ onSend, isSending, variant = "hero" }: NewChatInputProps) => {
  const [value, setValue] = useState("");

  const handleSend = () => {
    const text = value.trim();
    if (!text || isSending) return;
    onSend(text);
    setValue("");
  };

  const handleKeyDown = (e: KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  const isHero = variant === "hero";

  return (
    <div className={isHero ? "" : "border-t border-slate-200 bg-white px-6 py-4 dark:border-slate-700 dark:bg-slate-900"}>
      {isHero && (
        <div className="mb-6">
          <h1 className="text-2xl font-bold text-slate-900 dark:text-slate-100">무엇을 도와드릴까요?</h1>
          <p className="mt-1.5 text-sm text-slate-500 dark:text-slate-400">법률 용어를 몰라도 괜찮습니다. 평소 쓰는 말 그대로 적어주세요.</p>
        </div>
      )}

      <div className="rounded-2xl border border-violet-200 bg-white p-4 shadow-sm focus-within:border-violet-400 dark:border-violet-800 dark:bg-slate-800 dark:focus-within:border-violet-500">
        <textarea
          value={value}
          onChange={(e) => setValue(e.target.value.slice(0, MAX_LENGTH))}
          onKeyDown={handleKeyDown}
          rows={isHero ? 3 : 1}
          placeholder={isHero ? "예: 친구한테 500만원 빌려줬는데 안 갚아요" : "추가로 궁금한 점을 이어서 물어보세요"}
          className="w-full resize-none text-[15px] text-slate-800 outline-none placeholder:text-slate-400 dark:text-slate-100 dark:placeholder:text-slate-500"
        />
        <div className="mt-2 flex items-center justify-between">
          {isHero ? (
            <button type="button" className="flex items-center gap-1.5 text-sm text-slate-400 hover:text-slate-600 dark:hover:text-slate-300">
              <span aria-hidden>＋</span> 파일 첨부
            </button>
          ) : (
            <span />
          )}
          <div className="flex items-center gap-3">
            {isHero && <span className="text-xs text-slate-400 dark:text-slate-500">{value.length} / {MAX_LENGTH}</span>}
            <button
              type="button"
              onClick={handleSend}
              disabled={!value.trim() || isSending}
              className="rounded-xl bg-violet-600 px-5 py-2 text-sm font-semibold text-white transition-colors hover:bg-violet-700 disabled:cursor-not-allowed disabled:bg-slate-300 dark:disabled:bg-slate-700"
            >
              {isSending ? "답변 작성 중…" : isHero ? "질문하기" : "전송"}
            </button>
          </div>
        </div>
      </div>

      {isHero && (
        <div className="mt-6">
          <p className="mb-2.5 text-sm font-medium text-slate-700 dark:text-slate-300">이렇게 질문해 보세요</p>
          <div className="flex flex-wrap gap-2">
            {exampleQuestions.map((q) => (
              <button
                key={q}
                type="button"
                onClick={() => setValue(q)}
                className="rounded-full border border-slate-200 px-4 py-2 text-sm text-slate-600 transition-colors hover:border-violet-300 hover:bg-violet-50 hover:text-violet-700 dark:border-slate-700 dark:text-slate-300 dark:hover:border-violet-700 dark:hover:bg-violet-900/30 dark:hover:text-violet-300"
              >
                {q}
              </button>
            ))}
          </div>
        </div>
      )}

      <p className="mt-4 text-xs text-slate-400 dark:text-slate-500">
        이름·연락처·계좌번호 등은 자동으로 가려서 저장됩니다. · AI 답변은 참고 정보이며 법률 자문이 아닙니다.
      </p>
    </div>
  );
};