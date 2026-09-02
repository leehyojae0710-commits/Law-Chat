/**
 * 백엔드 com.lawchat.domain.precedent.dto.response 와 1:1 대응.
 * (PrecedentController 주석 참고: SearchBar/CategoryFilter/AiSimilaritySwitch -> GET /precedents)
 */

/** 목록 카드(PrecedentResultCard)용 요약 - PrecedentListResponse.PrecedentSummaryResponse */
export interface Precedent {
  id: string;
  court: string;
  /** "yyyy-MM-dd" 문자열. 재동기화 전 기존 데이터는 빈 문자열("")일 수 있음 - fallback 처리 필요 */
  decidedDate: string;
  caseNumber: string;
  title: string;
  summary: string;
  category: string;
}

/** GET /precedents 응답 - Precedent[] + 페이지 정보 (Spring Page 그대로가 아니라 items 필드 사용) */
export interface PrecedentListResponse {
  items: Precedent[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

/** GET /precedents/{id} 상세 - PrecedentDetailResponse */
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

/**
 * GET /precedents/{id}/ai-summary - PrecedentAiSummaryResponse
 * legal_chatbot_ai(KoBART) 실시간 요약. DB에 저장되지 않으므로 캐싱은 프론트 상태(컴포넌트 state)에서만 한다.
 */
export interface PrecedentAiSummary {
  summary: string;
  /** kobart 로딩은 됐지만 plain=false로 요청한 경우 등 null일 수 있음 */
  plainSummary: string | null;
}

/** GET /precedents/bookmarks - SavedPrecedentPanel용 - PrecedentBookmarkResponse */
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

/** GET /precedents 쿼리 파라미터 */
export interface PrecedentSearchParams {
  query?: string;
  category?: string;
  aiSimilarity?: boolean;
  page?: number;
  size?: number;
  caseNumber?: string;
  courtType?: CourtType;
  courtName?: string;
  /** "yyyy-MM-dd" */
  decidedDateFrom?: string;
  /** "yyyy-MM-dd" */
  decidedDateTo?: string;
}

export type CaseCategory = "전체" | "민사" | "형사" | "일반행정" | "가사" | "세무";
