import { apiClient } from "./client";

const naverApiKey = import.meta.env.VITE_NAVER_REST_API_KEY;
const naverRedirectUri = import.meta.env.VITE_NAVER_REDIRECT_URL;

export const getNaverAuthUrl = async (): Promise<string> => {
  const res = await apiClient.get<{ state: string }>("/auth/naver/state");
  const { state } = res.data;
  return `https://nid.naver.com/oauth2.0/authorize?response_type=code&client_id=${naverApiKey}&redirect_uri=${naverRedirectUri}&state=${encodeURIComponent(state)}`;
};