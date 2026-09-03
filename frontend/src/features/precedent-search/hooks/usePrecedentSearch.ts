import { useCallback, useEffect,useRef, useState } from "react";
import { searchPrecedents } from "../../../api/precedents";
import type { CaseCategory, Precedent, PrecedentSearchParams } from "../types";

const ALL_CATEGORY: CaseCategory = "전체";
const PAGE_SIZE = 10;

interface CachedResult {
  items: Precedent[];
  totalPages: number;
  totalElements: number;
}

export const usePrecedentSearch = () => {
  const [query, setQuery] = useState("");
  const [category, setCategory] = useState<CaseCategory>(ALL_CATEGORY);
  const [aiSimilarity, setAiSimilarity] = useState(true);
  const [page, setPage] = useState(0);

  const [items, setItems] = useState<Precedent[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const cacheRef = useRef<Map<string, CachedResult>>(new Map());

  const load = useCallback(async () => {
    const key = JSON.stringify({ query, category, aiSimilarity, page });
    const cached = cacheRef.current.get(key);

    if (cached) {
      // 캐시가 있으면 로딩 표시 없이 즉시 반영
      setItems(cached.items);
      setTotalPages(cached.totalPages);
      setTotalElements(cached.totalElements);
      setError(null);
      return;
    }

    setIsLoading(true);
    setError(null);
    try {
      const params: PrecedentSearchParams = {
        query: query || undefined,
        category: category === ALL_CATEGORY ? undefined : category,
        aiSimilarity,
        page,
        size: PAGE_SIZE,
      };
      const res = await searchPrecedents(params);
      cacheRef.current.set(key, {
        items: res.items,
        totalPages: res.totalPages,
        totalElements: res.totalElements,
      });
      setItems(res.items);
      setTotalPages(res.totalPages);
      setTotalElements(res.totalElements);
    } catch {
      setError("판례를 불러오지 못했어요. 잠시 후 다시 시도해주세요.");
      setItems([]);
    } finally {
      setIsLoading(false);
    }
  }, [query, category, aiSimilarity, page]);

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [query, category, aiSimilarity, page]);

  // 검색어는 입력 중이 아니라 SearchBar에서 제출(엔터/버튼)했을 때만 반영
  const search = useCallback((keyword: string) => {
    cacheRef.current.clear();
    setQuery(keyword);
    setPage(0);
  }, []);

  const selectCategory = useCallback((next: CaseCategory) => {
    cacheRef.current.clear();
    setCategory(next);
    setPage(0);
  }, []);

  return {
    query,
    search,
    category,
    selectCategory,
    aiSimilarity,
    setAiSimilarity,
    page,
    setPage,
    items,
    totalPages,
    totalElements,
    isLoading,
    error,
    reload: load,
  };
};
