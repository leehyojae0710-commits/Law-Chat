import { useState } from "react";
import type { Precedent, PrecedentDetail } from "../types";
import { getPrecedentDetail } from "../../../api/precedents";

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

  return (
    <div className="border rounded-lg p-4">
      <div className="flex items-start justify-between gap-3">
        <button type="button" className="text-left flex-1 min-w-0" onClick={toggleExpand}>
          <p className="text-xs text-gray-400 mb-1 break-words">
            {precedent.court}
            {precedent.decidedDate && ` · ${precedent.decidedDate} 선고`} · {precedent.caseNumber}
          </p>
          <p className="font-semibold mb-1 break-words">{precedent.title}</p>
          <p className={`text-sm text-gray-600 break-words ${expanded ? "" : "line-clamp-2"}`}>
            {precedent.summary}
          </p>
        </button>

        {isAuthenticated && (
          <button
            type="button"
            onClick={() => onToggleBookmark(precedent.id)}
            className={`shrink-0 text-xs px-2 py-1 rounded ${
              isBookmarked ? "bg-purple-600 text-white" : "border text-gray-500"
            }`}
          >
            {isBookmarked ? "저장됨" : "저장"}
          </button>
        )}
      </div>

      {expanded && (
        <div className="mt-3 pt-3 border-t text-sm space-y-2 min-w-0">
          {isLoading && <p className="text-gray-400">불러오는 중...</p>}
          {error && <p className="text-red-500">{error}</p>}
          {detail && (
            <>
              {detail.holding && (
                <div>
                  <p className="font-medium text-gray-700">판시사항</p>
                  <p className="text-gray-600 whitespace-pre-line break-words">{detail.holding}</p>
                </div>
              )}
              {detail.referencedArticles && (
                <div>
                  <p className="font-medium text-gray-700">참조조문</p>
                  <p className="text-gray-600 whitespace-pre-line break-words">{detail.referencedArticles}</p>
                </div>
              )}
              {detail.referencedCases && (
                <div>
                  <p className="font-medium text-gray-700">참조판례</p>
                  <p className="text-gray-600 whitespace-pre-line break-words">{detail.referencedCases}</p>
                </div>
              )}
            </>
          )}
        </div>
      )}
    </div>
  );
};
