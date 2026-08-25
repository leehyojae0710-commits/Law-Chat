import { useState } from "react";
import type { FeedbackReasonCode } from "../types";
import { feedbackReasons } from "../data";

interface FeedbackModalProps {
  onClose: () => void;
  onSubmit: (reasonCode: FeedbackReasonCode, detail?: string) => void;
}

export const FeedbackModal = ({ onClose, onSubmit }: FeedbackModalProps) => {
  const [selected, setSelected] = useState<FeedbackReasonCode>(feedbackReasons[0].code);
  const [detail, setDetail] = useState("");

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/40 px-4">
      <div className="w-full max-w-md rounded-2xl bg-white p-6 shadow-xl">
        <div className="mb-1 flex items-start justify-between">
          <h3 className="text-lg font-semibold text-slate-900">무엇이 아쉬웠나요?</h3>
          <button
            type="button"
            onClick={onClose}
            className="text-slate-400 hover:text-slate-600"
            aria-label="닫기"
          >
            ✕
          </button>
        </div>
        <p className="mb-4 text-sm text-slate-500">선택해 주신 내용은 답변 검수와 사전 개선에 사용됩니다.</p>

        <div className="space-y-2">
          {feedbackReasons.map((reason) => (
            <label
              key={reason.code}
              className={`flex cursor-pointer items-center gap-3 rounded-xl border px-4 py-3 text-sm transition-colors ${
                selected === reason.code
                  ? "border-violet-500 bg-violet-50 text-violet-700"
                  : "border-slate-200 text-slate-700 hover:bg-slate-50"
              }`}
            >
              <input
                type="radio"
                name="feedback-reason"
                className="accent-violet-600"
                checked={selected === reason.code}
                onChange={() => setSelected(reason.code)}
              />
              <span className="font-medium">{reason.label}</span>
            </label>
          ))}
        </div>

        <div className="mt-4">
          <label className="mb-1.5 block text-sm text-slate-600">자세히 알려주시면 큰 도움이 됩니다 (선택)</label>
          <textarea
            value={detail}
            onChange={(e) => setDetail(e.target.value)}
            rows={3}
            placeholder="어떤 부분이 아쉬웠는지 적어주세요"
            className="w-full resize-none rounded-xl border border-slate-200 px-3.5 py-2.5 text-sm outline-none focus:border-violet-400"
          />
        </div>

        <div className="mt-5 flex gap-2">
          <button
            type="button"
            onClick={onClose}
            className="flex-1 rounded-xl border border-slate-200 py-2.5 text-sm font-medium text-slate-700 hover:bg-slate-50"
          >
            취소
          </button>
          <button
            type="button"
            onClick={() => onSubmit(selected, detail.trim() || undefined)}
            className="flex-1 rounded-xl bg-violet-600 py-2.5 text-sm font-medium text-white hover:bg-violet-700"
          >
            보내기
          </button>
        </div>
      </div>
    </div>
  );
};
