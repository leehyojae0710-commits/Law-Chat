import { apiClient } from "./client";
import type {
  Precedent,
  PrecedentBookmark,
  PrecedentDetail,
  PrecedentListResponse,
  PrecedentSearchParams,
} from "../features/precedent-search/types";

// PrecedentController 매칭:
//  - SearchBar, CategoryFilter, AiSimilaritySwitch -> GET /precedents
//  - PrecedentResultCard 클릭(상세)                -> GET /precedents/{precedentId}
//  - SavedPrecedentPanel                           -> GET /precedents/bookmarks,
//                                                       POST/DELETE /precedents/{precedentId}/bookmark
// 목록/상세는 비로그인도 열람 가능. 북마크 3종은 로그인 필요(401 시 apiClient 인터셉터가 토큰을 붙이지 못하면 그대로 실패).

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
