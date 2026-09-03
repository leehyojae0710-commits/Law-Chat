import { useEffect, useState } from "react";
import { deleteInquiry, getInquiry } from "../../../api/inquiry";
import type { InquiryDetail } from "../types";
import { formatInquiryDate } from "../types";

interface InquiryThreadProps {
  inquiryId: number;
  onBack: () => void;
  onDeleted: () => void;
}

export const InquiryThread = ({ inquiryId, onBack, onDeleted }: InquiryThreadProps) => {
  const [detail, setDetail] = useState<InquiryDetail | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isDeleting, setIsDeleting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setIsLoading(true);
    setError(null);
    getInquiry(inquiryId)
      .then(setDetail)
      .catch((err) => {
        console.error("문의 상세 조회 실패:", err);
        setError("문의를 불러오지 못했습니다.");
      })
      .finally(() => setIsLoading(false));
  }, [inquiryId]);

  const handleDelete = async () => {
    if (!window.confirm("이 문의를 삭제할까요?")) return;
    setIsDeleting(true);
    try {
      await deleteInquiry(inquiryId);
      onDeleted();
    } catch (err) {
      console.error("문의 삭제 실패:", err);
      // 답변이 이미 등록된 뒤라 삭제가 막힌 경우(409) 등을 포함해 안내
      alert("삭제에 실패했습니다. 이미 답변이 등록된 문의는 삭제할 수 없어요.");
    } finally {
      setIsDeleting(false);
    }
  };

  return (
    <div className="border rounded-xl p-6 space-y-4">
      <button onClick={onBack} className="text-xs text-gray-400 hover:text-gray-600">
        ← 목록으로
      </button>

      {isLoading && <p className="text-sm text-gray-400">불러오는 중...</p>}
      {!isLoading && error && <p className="text-sm text-red-500">{error}</p>}

      {!isLoading && detail && (
        <>
          <div className="flex items-center gap-2">
            <span
              className={`text-xs px-2 py-0.5 rounded ${
                detail.status === "PENDING" ? "bg-amber-50 text-amber-600" : "bg-green-50 text-green-600"
              }`}
            >
              {detail.statusLabel}
            </span>
            <span className="text-xs text-gray-400">{detail.categoryLabel}</span>
          </div>

          <div>
            <p className="font-semibold">{detail.title}</p>
            <p className="text-xs text-gray-400 mt-1">{formatInquiryDate(detail.createdAt)}</p>
          </div>

          <p className="text-sm text-gray-700 whitespace-pre-wrap">{detail.content}</p>

          {detail.screenshotUrl && (
            <img src={detail.screenshotUrl} alt="첨부 스크린샷" className="max-w-xs rounded-lg border" />
          )}

          {detail.status === "ANSWERED" ? (
            <div className="bg-violet-50 rounded-lg p-4">
              <p className="text-xs font-medium text-violet-600 mb-1">답변</p>
              <p className="text-sm text-gray-700 whitespace-pre-wrap">{detail.answerContent}</p>
              {detail.answeredAt && (
                <p className="text-xs text-gray-400 mt-2">{formatInquiryDate(detail.answeredAt)}</p>
              )}
            </div>
          ) : (
            <p className="text-sm text-gray-400">아직 답변이 등록되지 않았어요.</p>
          )}

          {detail.status === "PENDING" && (
            <button
              onClick={handleDelete}
              disabled={isDeleting}
              className="text-xs text-red-500 disabled:opacity-50"
            >
              {isDeleting ? "삭제 중..." : "문의 삭제"}
            </button>
          )}
        </>
      )}
    </div>
  );
};