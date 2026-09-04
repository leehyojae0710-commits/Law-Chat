/**
 * 백엔드 com.lawchat.domain.precedent.dto.response 와 1:1 대응.
 */

export interface Precedent {
  id: string;
  court: string;
  decidedDate: string;
  caseNumber: string;
  title: string;
  summary: string;
  category: string;
}

export interface PrecedentListResponse {
  items: Precedent[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface PrecedentDetail {
  id: string;
  caseNumber: string;
  title: string;
  court: string;
  decidedDate: string;
  category: string;
  holding: string;
  summary: string;
  referencedArticles: string;
  referencedCases: string;
  fullText: string;
  syncedAt: string;
  isBookmarked: boolean;
}

export interface PrecedentAiSummary {
  summary: string;
  plainSummary: string | null;
}

export interface PrecedentBookmark {
  bookmarkId: number;
  precedentId: string;
  court: string;
  caseNumber: string;
  title: string;
  category: string;
  bookmarkedAt: string;
}

/** courtType은 이 3개 중 하나가 아니면 백엔드에서 무시됨 (PrecedentSearchService 참고) */
export type CourtType = "대법원" | "고등법원" | "하급심";

/** GET /precedents 쿼리 파라미터 - category/courtType은 다중선택이라 배열로 나간다 (0개=필터 없음) */
export interface PrecedentSearchParams {
  query?: string;
  category?: CaseCategory[];
  aiSimilarity?: boolean;
  page?: number;
  size?: number;
  caseNumber?: string;
  caseName?: string;
  referencedArticles?: string;
  courtType?: CourtType[];
  courtName?: string;
  decidedDateFrom?: string;
  decidedDateTo?: string;
}

export type CaseCategory = "전체" | "민사" | "형사" | "일반행정" | "가사" | "세무" | "특허" | "선거,특별";