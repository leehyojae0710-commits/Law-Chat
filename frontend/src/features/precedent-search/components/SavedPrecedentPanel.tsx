import type { PrecedentBookmark } from "../types";

interface SavedPrecedentPanelProps {
  isAuthenticated: boolean;
  bookmarks: PrecedentBookmark[];
  isLoading: boolean;
  error: string | null;
}

export const SavedPrecedentPanel = ({
  isAuthenticated,
  bookmarks,
  isLoading,
  error,
}: SavedPrecedentPanelProps) => {
  return (
    <div className="border rounded-xl p-4 h-fit">
      <p className="font-semibold mb-1">저장한 판례</p>

      {!isAuthenticated && (
        <p className="text-xs text-gray-400">로그인하면 검색 결과를 저장할 수 있어요</p>
      )}

      {isAuthenticated && (
        <>
          {isLoading && <p className="text-xs text-gray-400">불러오는 중...</p>}
          {!isLoading && error && <p className="text-xs text-red-500">{error}</p>}
          {!isLoading && !error && bookmarks.length === 0 && (
            <p className="text-xs text-gray-400">아직 저장한 판례가 없어요</p>
          )}
          {!isLoading && !error && bookmarks.length > 0 && (
            <ul className="mt-3 space-y-3 min-w-0">
              {bookmarks.map((b) => (
                <li key={b.bookmarkId} className="text-sm">
                  <p className="text-xs text-gray-400">
                    {b.court} · {b.caseNumber}
                  </p>
                  <p className="font-medium break-words">{b.title}</p>
                </li>
              ))}
            </ul>
          )}
        </>
      )}
    </div>
  );
};
