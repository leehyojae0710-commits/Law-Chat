import { useAuthStore } from "../store/authStore";
import { usePrecedentSearch } from "../features/precedent-search/hooks/usePrecedentSearch";
import { useBookmarks } from "../features/precedent-search/hooks/useBookmarks";
import { SearchBar } from "../features/precedent-search/components/SearchBar";
import { AiSimilaritySwitch } from "../features/precedent-search/components/AiSimilaritySwitch";
import { CategoryFilter, precedentCategories } from "../features/precedent-search/components/CategoryFilter";
import { PrecedentResultCard } from "../features/precedent-search/components/PrecedentResultCard";
import { SavedPrecedentPanel } from "../features/precedent-search/components/SavedPrecedentPanel";
import { Disclaimer } from "../components/layout/Disclaimer";

export const PrecedentSearchPage = () => {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);

  const {
    category,
    selectCategory,
    aiSimilarity,
    setAiSimilarity,
    search,
    page,
    setPage,
    items,
    totalPages,
    totalElements,
    isLoading,
    error,
  } = usePrecedentSearch();

  const {
    bookmarks,
    isLoading: isBookmarksLoading,
    error: bookmarksError,
    isBookmarked,
    toggleBookmark,
  } = useBookmarks();

  return (
    <div className="min-h-screen bg-violet-50 py-5">
      <div className="max-w-6xl mx-auto px-8 py-10 grid grid-cols-[minmax(0,1fr)_320px] gap-6 ... bg-white rounded-xl shadow-sm">
        <div className="space-y-4 min-w-0">
          <div>
            <h1 className="text-2xl font-bold">판례 검색</h1>
            <p className="text-gray-600 mt-1">
              국가법령정보 공동활용 「판례 목록 조회」 API로 정확한 판례를 제공합니다.
            </p>
          </div>

          <SearchBar onSearch={search} />
          <AiSimilaritySwitch checked={aiSimilarity} onChange={setAiSimilarity} />
          <CategoryFilter categories={precedentCategories} active={category} onSelect={selectCategory} />

          {isLoading && <p className="text-sm text-gray-400">불러오는 중...</p>}
          {!isLoading && error && <p className="text-sm text-red-500">{error}</p>}
          {!isLoading && !error && items.length === 0 && (
            <p className="text-sm text-gray-400">검색 결과가 없어요</p>
          )}

          {!isLoading && !error && items.length > 0 && (
            <>
              <p className="text-xs text-gray-400">총 {totalElements.toLocaleString()}건</p>
              <div className="space-y-3">
                {items.map((p) => (
                  <PrecedentResultCard
                    key={p.id}
                    precedent={p}
                    isAuthenticated={isAuthenticated}
                    isBookmarked={isBookmarked(p.id)}
                    onToggleBookmark={toggleBookmark}
                  />
                ))}
              </div>
            </>
          )}

          {totalPages > 1 && (() => {
            const pageSize = 10;
            const windowStart = Math.floor(page / pageSize) * pageSize;
            const windowEnd = Math.min(windowStart + pageSize, totalPages);
            const pageNumbers = Array.from(
              { length: windowEnd - windowStart },
              (_, i) => windowStart + i
            );

            return (
              <div className="flex justify-center items-center gap-2 pt-4">
                <button
                  onClick={() => setPage(Math.max(windowStart - pageSize, 0))}
                  disabled={windowStart === 0}
                  className="w-8 h-8 rounded-full text-sm text-gray-500 hover:bg-gray-100 disabled:opacity-30"
                >
                  ‹
                </button>

                {pageNumbers.map((i) => (
                  <button
                    key={i}
                    onClick={() => setPage(i)}
                    className={`w-8 h-8 rounded-full text-sm shrink-0 ${page === i ? "bg-purple-600 text-white" : "text-gray-500 hover:bg-gray-100"
                      }`}
                  >
                    {i + 1}
                  </button>
                ))}

                <button
                  onClick={() => setPage(Math.min(windowStart + pageSize, totalPages - 1))}
                  disabled={windowEnd >= totalPages}
                  className="w-8 h-8 rounded-full text-sm text-gray-500 hover:bg-gray-100 disabled:opacity-30"
                >
                  ›
                </button>
              </div>
            );
          })()}

          <Disclaimer />
        </div>

        <SavedPrecedentPanel
          isAuthenticated={isAuthenticated}
          bookmarks={bookmarks}
          isLoading={isBookmarksLoading}
          error={bookmarksError}
        />
      </div>
    </div>
  );
};
