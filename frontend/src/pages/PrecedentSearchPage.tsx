import { useAuthStore } from "../store/authStore";
import { usePrecedentSearch } from "../features/precedent-search/hooks/usePrecedentSearch";
import { useBookmarks } from "../features/precedent-search/hooks/useBookmarks";
import { SearchBar } from "../features/precedent-search/components/SearchBar";
import { FieldSearchInput } from "../features/precedent-search/components/FieldSearchInput";
import { CategoryFilter } from "../features/precedent-search/components/CategoryFilter";
import { CourtTypeFilter } from "../features/precedent-search/components/CourtTypeFilter";
import { CourtNameSelect } from "../features/precedent-search/components/CourtNameSelect";
import { DateRangeFilter } from "../features/precedent-search/components/DateRangeFilter";
import { PrecedentResultCard } from "../features/precedent-search/components/PrecedentResultCard";
import { SavedPrecedentPanel } from "../features/precedent-search/components/SavedPrecedentPanel";
import { Disclaimer } from "../components/layout/Disclaimer";

export const PrecedentSearchPage = () => {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);

  const {
    caseNumber,
    searchCaseNumber,
    caseName,
    searchCaseName,
    referencedArticles,
    searchReferencedArticles,
    categories,
    selectCategories,
    courtTypes,
    selectCourtTypes,
    courtName,
    selectCourtName,
    decidedDateFrom,
    selectDecidedDateFrom,
    decidedDateTo,
    selectDecidedDateTo,
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

          <div className="grid grid-cols-1 md:grid-cols-[240px_minmax(0,1fr)] gap-x-8 gap-y-4 pt-4 border-t">
            <div className="space-y-3">
              <FieldSearchInput
                label="사건번호"
                placeholder="예) 2023가합1234"
                value={caseNumber}
                onSearch={searchCaseNumber}
              />
              <FieldSearchInput
                label="사건명"
                placeholder="예) 손해배상"
                value={caseName}
                onSearch={searchCaseName}
              />
              <FieldSearchInput
                label="참조조문"
                placeholder="예) 민법 제3조"
                value={referencedArticles}
                onSearch={searchReferencedArticles}
              />
            </div>

            <div className="space-y-3">
              <CourtTypeFilter selected={courtTypes} onChange={selectCourtTypes} />
              <CategoryFilter selected={categories} onChange={selectCategories} />
              <CourtNameSelect value={courtName} onChange={selectCourtName} />
              <DateRangeFilter
                from={decidedDateFrom}
                to={decidedDateTo}
                onChangeFrom={selectDecidedDateFrom}
                onChangeTo={selectDecidedDateTo}
              />
            </div>
          </div>

          {isLoading && items.length === 0 && (
            <p className="text-sm text-gray-400">불러오는 중...</p>
          )}
          {!isLoading && error && <p className="text-sm text-red-500">{error}</p>}
          {!isLoading && !error && items.length === 0 && (
            <p className="text-sm text-gray-400">검색 결과가 없어요</p>
          )}

          {items.length > 0 && (
            <>
              <p className="text-xs text-gray-400">
                총 {totalElements.toLocaleString()}건
                {isLoading && <span className="ml-2 text-gray-300">불러오는 중...</span>}
              </p>
              <div className={`space-y-3 ${isLoading ? "opacity-50" : ""}`}>
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
