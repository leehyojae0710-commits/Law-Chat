import { apiClient } from "./client";
import type {
  Precedent,
  PrecedentAiSummary,
  PrecedentBookmark,
  PrecedentDetail,
  PrecedentListResponse,
  PrecedentSearchParams,
} from "../features/precedent-search/types";

// PrecedentController 매칭:
//  - SearchBar, CategoryFilter, AiSimilaritySwitch -> GET /precedents
//  - PrecedentResultCard 클릭(상세)                -> GET /precedents/{precedentId}
//  - PrecedentResultCard "AI 요약 보기"             -> GET /precedents/{precedentId}/ai-summary
//  - SavedPrecedentPanel                           -> GET /precedents/bookmarks,
//                                                       POST/DELETE /precedents/{precedentId}/bookmark
// 목록/상세/AI요약은 비로그인도 열람 가능. 북마크 3종은 로그인 필요(401 시 apiClient 인터셉터가 토큰을 붙이지 못하면 그대로 실패).

export const searchPrecedents = async (
  params: PrecedentSearchParams
): Promise<PrecedentListResponse> => {
  const res = await apiClient.get<PrecedentListResponse>("/precedents", { params });
  return res.data;
};

export const getPrecedentDetail = async (
  precedentId: Precedent["id"]
): Promise<PrecedentDetail> => {
  const res = await apiClient.get<PrecedentDetail>(`/precedents/${precedentId}`);
  return res.data;
};

// AI(KoBART) 요약은 매 호출마다 legal_chatbot_ai를 실시간으로 태우므로(수 초 소요될 수 있음)
// PrecedentResultCard에서 버튼을 눌렀을 때만 호출하고, 받은 결과는 컴포넌트 state에 캐싱해서 재클릭 시 재요청하지 않는다.
export const getPrecedentAiSummary = async (
  precedentId: Precedent["id"]
): Promise<PrecedentAiSummary> => {
  const res = await apiClient.get<PrecedentAiSummary>(`/precedents/${precedentId}/ai-summary`);
  return res.data;
};

export const getBookmarks = async (): Promise<PrecedentBookmark[]> => {
  const res = await apiClient.get<PrecedentBookmark[]>("/precedents/bookmarks");
  return res.data;
};

export const addBookmark = async (precedentId: Precedent["id"]): Promise<void> => {
  await apiClient.post(`/precedents/${precedentId}/bookmark`);
};

export const removeBookmark = async (precedentId: Precedent["id"]): Promise<void> => {
  await apiClient.delete(`/precedents/${precedentId}/bookmark`);
};
