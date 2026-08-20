import { useState } from "react";
import { useInquiries } from "../hooks/useInquiries";

export const InquiryQueue = () => {
  const { inquiries, isLoading, submitAnswer } = useInquiries();
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [answerText, setAnswerText] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  const selected = inquiries.find((i) => i.id === selectedId);

  const handleSubmit = async () => {
    if (!selectedId || !answerText.trim()) return;
    setIsSubmitting(true);
    try {
      await submitAnswer(selectedId, answerText);
      setAnswerText("");
      setSelectedId(null);
    } finally {
      setIsSubmitting(false);
    }
  };

  if (isLoading) return <p className="text-sm text-gray-400">불러오는 중...</p>;

  return (
    <div className="grid grid-cols-[1fr_1fr] gap-4">
      <div className="border rounded-xl divide-y">
        {inquiries.map((inq) => (
          <button
            key={inq.id}
            onClick={() => setSelectedId(inq.id)}
            className={`w-full text-left p-4 ${selectedId === inq.id ? "bg-violet-50" : ""}`}
          >
            <div className="flex items-center gap-2 mb-1">
              <span
                className={`text-xs px-2 py-0.5 rounded ${
                  inq.status === "미답변" ? "bg-red-50 text-red-500" : "bg-green-50 text-green-600"
                }`}
              >
                {inq.status}
              </span>
              <p className="font-medium text-sm">{inq.title}</p>
            </div>
            <p className="text-xs text-gray-400">{inq.authorEmail} · {inq.createdAt}</p>
          </button>
        ))}
      </div>

      <div className="border rounded-xl p-4">
        {!selected ? (
          <p className="text-sm text-gray-400">문의를 선택해주세요.</p>
        ) : (
          <div className="space-y-4">
            <div>
              <p className="font-semibold text-sm">{selected.title}</p>
              <p className="text-sm text-gray-600 mt-2">{selected.content}</p>
            </div>
            <textarea
              value={answerText}
              onChange={(e) => setAnswerText(e.target.value)}
              placeholder="답변 내용을 입력하세요..."
              rows={5}
              className="w-full border rounded-lg p-3 text-sm"
            />
            <button
              onClick={handleSubmit}
              disabled={isSubmitting || !answerText.trim()}
              className="px-4 py-2 rounded-lg bg-violet-600 text-white text-sm font-medium disabled:opacity-50"
            >
              {isSubmitting ? "등록 중..." : "답변 등록"}
            </button>
          </div>
        )}
      </div>
    </div>
  );
};
