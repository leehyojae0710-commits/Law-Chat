import { apiClient } from "./client";
import type {
  Precedent,
  PrecedentAiSummary,
  PrecedentBookmark,
  PrecedentDetail,
  PrecedentListResponse,
  PrecedentSearchParams,
} from "../features/precedent-search/types";

export const searchPrecedents = async (
  params: PrecedentSearchParams
): Promise<PrecedentListResponse> => {
  const res = await apiClient.get<PrecedentListResponse>("/precedents", { params });
  return res.data;
};

// 법원명 드롭다운(CourtNameSelect)용 - DB에 실제 존재하는 법원명 전체 목록.
export const getCourtNames = async (): Promise<string[]> => {
  const res = await apiClient.get<string[]>("/precedents/court-names");
  return res.data;
};

export const getPrecedentDetail = async (
  precedentId: Precedent["id"]
): Promise<PrecedentDetail> => {
  const res = await apiClient.get<PrecedentDetail>(`/precedents/${precedentId}`);
  return res.data;
};

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