import { apiClient } from "./client";
import type { LoginPayload, SignupPayload, AuthUser } from "../features/auth/types";

export interface AuthResponse {
  accessToken: string;
  user: AuthUser;
}

export const login = async (payload: LoginPayload): Promise<AuthResponse> => {
  const res = await apiClient.post<AuthResponse>("/auth/login", payload);
  return res.data;
};

export const signup = async (payload: SignupPayload): Promise<AuthResponse> => {
  const res = await apiClient.post<AuthResponse>("/auth/signup", payload);
  return res.data;
};

export const getMe = async (): Promise<AuthUser> => {
  const res = await apiClient.get<AuthUser>("/auth/me");
  return res.data;
};

export const logoutApi = async (): Promise<void> => {
  await apiClient.post("/auth/logout");
};


export const kakaoLogin = async (code: string): Promise<AuthResponse> => {
  const res = await apiClient.post<AuthResponse>("/auth/kakao", { code });
  return res.data;
};


export const naverLogin = async (code: string): Promise<AuthResponse> => {
  const res = await apiClient.post<AuthResponse>("/auth/naver", { code });
  return res.data;
};

