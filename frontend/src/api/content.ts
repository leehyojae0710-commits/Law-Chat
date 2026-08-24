import { apiClient } from "./client";

export interface MainPageContent {
  heroTitleLine1: string;
  heroTitleLine2: string;
  heroDescription: string;
  updatedAt: string;
}

// 유저가 보는 랜딩페이지는 이 API를 호출합니다.
// 백엔드는 "게시(publish)된" 최신 버전만 반환합니다 (초안/draft는 절대 여기 안 나옴).
export const getMainPageContent = async (): Promise<MainPageContent> => {
  const res = await apiClient.get<MainPageContent>("/content/main");
  return res.data;
};
