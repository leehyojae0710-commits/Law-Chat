import { useState } from "react";
import { useInquiries } from "../hooks/useInquiries";
import { INQUIRY_CATEGORY_LABELS, INQUIRY_STATUS_LABELS, formatInquiryDate } from "../../support/types";
import type { InquiryCategory, InquiryStatus } from "../../support/types";

const CATEGORY_OPTIONS = Object.keys(INQUIRY_CATEGORY_LABELS) as InquiryCategory[];
const STATUS_OPTIONS = Object.keys(INQUIRY_STATUS_LABELS) as InquiryStatus[];

export const InquiryQueue = () => {
  const {
    inquiries,
    isLoading,
    page,
    setPage,
    totalPages,
    statusFilter,
    setStatusFilter,
    categoryFilter,
    setCategoryFilter,
    submitAnswer,
  } = useInquiries();

  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [answerText, setAnswerText] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  const selected = inquiries.find((i) => i.inquiryId === selectedId);

  const handleSelect = (inquiryId: number) => {
    setSelectedId(inquiryId);
    const target = inquiries.find((i) => i.inquiryId === inquiryId);
    setAnswerText(target?.answerContent ?? ""); // 이미 답변이 있으면 이어서 수정할 수 있게 불러옴
  };

  const handleSubmit = async () => {
    if (!selectedId || !answerText.trim()) return;
    setIsSubmitting(true);
    try {
      await submitAnswer(selectedId, answerText.trim());
      setAnswerText("");
      setSelectedId(null);
    } catch (err) {
      console.error("답변 등록 실패:", err);
      alert("답변 등록에 실패했습니다. 다시 시도해주세요.");
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="space-y-4">
      {/* 필터 */}
      <div className="flex flex-wrap items-center gap-2">
        <select
          value={statusFilter ?? ""}
          onChange={(e) => setStatusFilter(e.target.value ? (e.target.value as InquiryStatus) : undefined)}
          className="border rounded-lg px-2 py-1.5 text-sm"
        >
          <option value="">전체 상태</option>
          {STATUS_OPTIONS.map((s) => (
            <option key={s} value={s}>
              {INQUIRY_STATUS_LABELS[s]}
            </option>
          ))}
        </select>
        <select
          value={categoryFilter ?? ""}
          onChange={(e) => setCategoryFilter(e.target.value ? (e.target.value as InquiryCategory) : undefined)}
          className="border rounded-lg px-2 py-1.5 text-sm"
        >
          <option value="">전체 유형</option>
          {CATEGORY_OPTIONS.map((c) => (
            <option key={c} value={c}>
              {INQUIRY_CATEGORY_LABELS[c]}
            </option>
          ))}
        </select>
      </div>

      {isLoading ? (
        <p className="text-sm text-gray-400">불러오는 중...</p>
      ) : inquiries.length === 0 ? (
        <p className="text-sm text-gray-400 border rounded-xl p-10 text-center">조건에 맞는 문의가 없습니다.</p>
      ) : (
        <div className="grid grid-cols-[1fr_1fr] gap-4">
          <div className="border rounded-xl divide-y">
            {inquiries.map((inq) => (
              <button
                key={inq.inquiryId}
                onClick={() => handleSelect(inq.inquiryId)}
                className={`w-full text-left p-4 ${selectedId === inq.inquiryId ? "bg-violet-50" : ""}`}
              >
                <div className="flex items-center gap-2 mb-1">
                  <span
                    className={`text-xs px-2 py-0.5 rounded ${
                      inq.status === "PENDING" ? "bg-amber-50 text-amber-600" : "bg-green-50 text-green-600"
                    }`}
                  >
                    {inq.statusLabel}
                  </span>
                  <span className="text-xs text-gray-400">{inq.categoryLabel}</span>
                  <p className="font-medium text-sm">{inq.title}</p>
                </div>
                <p className="text-xs text-gray-400">
                  {inq.authorEmail ?? inq.authorNickname ?? "탈퇴한 회원"} · {formatInquiryDate(inq.createdAt)}
                </p>
              </button>
            ))}
          </div>

          <div className="border rounded-xl p-4">
            {!selected ? (
              <p className="text-sm text-gray-400">문의를 선택해주세요.</p>
            ) : (
              <div className="space-y-4">
                <div>
                  <div className="flex items-center gap-2 mb-1">
                    <span className="text-xs px-2 py-0.5 rounded bg-gray-100 text-gray-500">
                      {selected.categoryLabel}
                    </span>
                    <p className="font-semibold text-sm">{selected.title}</p>
                  </div>
                  <p className="text-xs text-gray-400">
                    {selected.authorEmail ?? selected.authorNickname ?? "탈퇴한 회원"} ·{" "}
                    {formatInquiryDate(selected.createdAt)}
                  </p>
                  <p className="text-sm text-gray-600 mt-2 whitespace-pre-wrap">{selected.content}</p>
                  {selected.screenshotUrl && (
                    <img
                      src={selected.screenshotUrl}
                      alt="첨부 스크린샷"
                      className="mt-2 max-w-[200px] rounded-lg border"
                    />
                  )}
                </div>
                <textarea
                  value={answerText}
                  onChange={(e) => setAnswerText(e.target.value)}
                  placeholder="답변 내용을 입력하세요..."
                  maxLength={4000}
                  rows={5}
                  className="w-full border rounded-lg p-3 text-sm"
                />
                <button
                  onClick={handleSubmit}
                  disabled={isSubmitting || !answerText.trim()}
                  className="px-4 py-2 rounded-lg bg-violet-600 text-white text-sm font-medium disabled:opacity-50"
                >
                  {isSubmitting ? "등록 중..." : selected.status === "ANSWERED" ? "답변 수정" : "답변 등록"}
                </button>
              </div>
            )}
          </div>
        </div>
      )}

      {/* 페이지네이션 바 */}
      {totalPages > 1 && (
        <div className="flex items-center justify-center gap-1">
          <button
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            disabled={page === 0}
            className="px-2 py-1 text-sm text-gray-500 disabled:opacity-30"
          >
            ‹
          </button>
          {Array.from({ length: totalPages }, (_, i) => (
            <button
              key={i}
              onClick={() => setPage(i)}
              className={`w-7 h-7 rounded-lg text-sm ${
                i === page ? "bg-violet-600 text-white font-medium" : "text-gray-500 hover:bg-violet-50"
              }`}
            >
              {i + 1}
            </button>
          ))}
          <button
            onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
            disabled={page === totalPages - 1}
            className="px-2 py-1 text-sm text-gray-500 disabled:opacity-30"
          >
            ›
          </button>
        </div>
      )}
    </div>
  );
};