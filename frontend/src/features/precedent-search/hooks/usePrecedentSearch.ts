import { useCallback, useEffect, useRef, useState } from "react";
import { searchPrecedents } from "../../../api/precedents";
import type { CaseCategory, CourtType, Precedent, PrecedentSearchParams } from "../types";

const PAGE_SIZE = 10;

interface CachedResult {
  items: Precedent[];
  totalPages: number;
  totalElements: number;
}

// 캐시 키는 선택 "순서"가 아니라 "무엇이 선택됐는지"로만 갈려야 하므로 정렬 후 직렬화한다.
const sortedKey = (arr: string[]) => [...arr].sort();

export const usePrecedentSearch = () => {
  const [query, setQuery] = useState("");
  const [caseNumber, setCaseNumberState] = useState("");
  const [caseName, setCaseNameState] = useState("");
  const [referencedArticles, setReferencedArticlesState] = useState("");
  const [categories, setCategoriesState] = useState<CaseCategory[]>([]);
  const [courtTypes, setCourtTypesState] = useState<CourtType[]>([]);
  const [courtName, setCourtNameState] = useState<string | undefined>(undefined);
  // yyyy-MM-dd 또는 빈 문자열(=미지정). <input type="date">가 이 형식을 그대로 주고받는다.
  const [decidedDateFrom, setDecidedDateFromState] = useState("");
  const [decidedDateTo, setDecidedDateToState] = useState("");
  const [aiSimilarity, setAiSimilarity] = useState(true);
  const [page, setPage] = useState(0);

  const [items, setItems] = useState<Precedent[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const cacheRef = useRef<Map<string, CachedResult>>(new Map());

  const load = useCallback(async () => {
    const key = JSON.stringify({
      query,
      caseNumber,
      caseName,
      referencedArticles,
      categories: sortedKey(categories),
      courtTypes: sortedKey(courtTypes),
      courtName,
      decidedDateFrom,
      decidedDateTo,
      aiSimilarity,
      page,
    });
    const cached = cacheRef.current.get(key);

    if (cached) {
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
        caseNumber: caseNumber || undefined,
        caseName: caseName || undefined,
        referencedArticles: referencedArticles || undefined,
        category: categories.length > 0 ? categories : undefined,
        courtType: courtTypes.length > 0 ? courtTypes : undefined,
        courtName,
        decidedDateFrom: decidedDateFrom || undefined,
        decidedDateTo: decidedDateTo || undefined,
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
  }, [query, caseNumber, caseName, referencedArticles, categories, courtTypes, courtName, decidedDateFrom, decidedDateTo, aiSimilarity, page]);

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [query, caseNumber, caseName, referencedArticles, categories, courtTypes, courtName, decidedDateFrom, decidedDateTo, aiSimilarity, page]);

  const search = useCallback((keyword: string) => {
    cacheRef.current.clear();
    setQuery(keyword);
    setPage(0);
  }, []);

  const searchCaseNumber = useCallback((value: string) => {
    cacheRef.current.clear();
    setCaseNumberState(value.trim());
    setPage(0);
  }, []);

  const searchCaseName = useCallback((value: string) => {
    cacheRef.current.clear();
    setCaseNameState(value.trim());
    setPage(0);
  }, []);

  const searchReferencedArticles = useCallback((value: string) => {
    cacheRef.current.clear();
    setReferencedArticlesState(value.trim());
    setPage(0);
  }, []);

  const selectCategories = useCallback((next: CaseCategory[]) => {
    cacheRef.current.clear();
    setCategoriesState(next);
    setPage(0);
  }, []);

  const selectCourtTypes = useCallback((next: CourtType[]) => {
    cacheRef.current.clear();
    setCourtTypesState(next);
    setPage(0);
  }, []);

  const selectCourtName = useCallback((next: string | undefined) => {
    cacheRef.current.clear();
    setCourtNameState(next);
    setPage(0);
  }, []);

  const selectDecidedDateFrom = useCallback((next: string) => {
    cacheRef.current.clear();
    setDecidedDateFromState(next);
    setPage(0);
  }, []);

  const selectDecidedDateTo = useCallback((next: string) => {
    cacheRef.current.clear();
    setDecidedDateToState(next);
    setPage(0);
  }, []);

  return {
    query,
    search,
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