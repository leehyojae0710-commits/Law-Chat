import { useState } from "react";
import type { Precedent, PrecedentAiSummary, PrecedentDetail } from "../types";
import { getPrecedentAiSummary, getPrecedentDetail } from "../../../api/precedents";
import { toLineBreaks, truncateText } from "../hooks/usePrecedentTextFome.ts";

interface PrecedentResultCardProps {
  precedent: Precedent;
  isAuthenticated: boolean;
  isBookmarked: boolean;
  onToggleBookmark: (precedentId: Precedent["id"]) => void;
}

export const PrecedentResultCard = ({
  precedent,
  isAuthenticated,
  isBookmarked,
  onToggleBookmark,
}: PrecedentResultCardProps) => {
  const [expanded, setExpanded] = useState(false);
  const [detail, setDetail] = useState<PrecedentDetail | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // AI(KoBART) 요약: 상세와 별개로 버튼을 눌렀을 때만 호출한다 (몇 초 걸릴 수 있음).
  // 한 번 받아오면 state에 캐싱해서 다시 누를 때 재요청하지 않는다.
  const [aiSummary, setAiSummary] = useState<PrecedentAiSummary | null>(null);
  const [isAiSummaryLoading, setIsAiSummaryLoading] = useState(false);
  const [aiSummaryError, setAiSummaryError] = useState<string | null>(null);

  const toggleExpand = async () => {
    const next = !expanded;
    setExpanded(next);
    if (next && !detail) {
      setIsLoading(true);
      setError(null);
      try {
        setDetail(await getPrecedentDetail(precedent.id));
      } catch {
        setError("상세 내용을 불러오지 못했어요.");
      } finally {
        setIsLoading(false);
      }
    }
  };

  const loadAiSummary = async () => {
    if (aiSummary || isAiSummaryLoading) return;
    setIsAiSummaryLoading(true);
    setAiSummaryError(null);
    try {
      setAiSummary(await getPrecedentAiSummary(precedent.id));
    } catch {
      setAiSummaryError("AI 요약을 불러오지 못했어요. 잠시 후 다시 시도해 주세요.");
    } finally {
      setIsAiSummaryLoading(false);
    }
  };

  return (
    <div className="border rounded-lg p-4">
      <div className="flex items-start justify-between gap-3">
        <button type="button" className="text-left flex-1 min-w-0" onClick={toggleExpand}>
          <p className="text-xs text-gray-400 mb-1">
            {precedent.court}
            {precedent.decidedDate && ` · ${precedent.decidedDate} 선고`} · {precedent.caseNumber}
          </p>
          <p className="font-semibold mb-1 break-words">{precedent.title}</p>
          <p className="text-sm text-gray-600 break-words whitespace-pre-line">
            {precedent.summary? toLineBreaks(truncateText(precedent.summary, 100)):""}
          </p>
        </button>

        {isAuthenticated && (
          <button
            type="button"
            onClick={() => onToggleBookmark(precedent.id)}
            className={`shrink-0 text-xs px-2 py-1 rounded ${isBookmarked ? "bg-purple-600 text-white" : "border text-gray-500"
              }`}
          >
            {isBookmarked ? "저장됨" : "저장"}
          </button>
        )}
      </div>

      {expanded && (
        <div className="mt-3 pt-3 border-t text-sm space-y-2">
          {isLoading && <p className="text-gray-400">불러오는 중...</p>}
          {error && <p className="text-red-500">{error}</p>}
          {detail && (
            <>
              {detail.holding && (
                <div>
                  <p className="font-medium text-gray-700">판시사항</p>
                  <p className="text-gray-600 whitespace-pre-line break-words">{toLineBreaks(detail.holding)}</p>
                </div>
              )}
              {detail.referencedArticles && (
                <div>
                  <p className="font-medium text-gray-700">참조조문</p>
                  <p className="text-gray-600 whitespace-pre-line break-words">{toLineBreaks(detail.referencedArticles)}</p>
                </div>
              )}
              {detail.referencedCases && (
                <div>
                  <p className="font-medium text-gray-700">참조판례</p>
                  <p className="text-gray-600 whitespace-pre-line break-words">{toLineBreaks(detail.referencedCases)}</p>
                </div>
              )}

              <div className="pt-2">
                {!aiSummary && (
                  <button
                    type="button"
                    onClick={loadAiSummary}
                    disabled={isAiSummaryLoading}
                    className="text-xs px-2 py-1 rounded border border-purple-500 text-purple-600 disabled:opacity-50"
                  >
                    {isAiSummaryLoading ? "AI 요약 생성 중..." : "AI 요약 보기"}
                  </button>
                )}
                {aiSummaryError && <p className="text-red-500 mt-1">{aiSummaryError}</p>}
                {aiSummary && (
                  <div className="mt-1 space-y-2">
                    <div>
                      <p className="font-medium text-gray-700">AI 요약</p>
                      <p className="text-gray-600 whitespace-pre-line">{aiSummary.summary}</p>
                    </div>
                    {aiSummary.plainSummary && (
                      <div>
                        <p className="font-medium text-gray-700">쉬운 설명</p>
                        <p className="text-gray-600 whitespace-pre-line">{aiSummary.plainSummary}</p>
                      </div>
                    )}
                  </div>
                )}
              </div>
            </>
          )}
        </div>
      )}
    </div>
  );
};
