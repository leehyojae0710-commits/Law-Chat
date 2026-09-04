import { apiClient } from "./client";
import type { LoginPayload, SignupPayload, AuthUser, AuthUser_Profile } from "../features/auth/types";

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

export const getMe = async (): Promise<AuthUser_Profile> => {
  const res = await apiClient.get<AuthUser_Profile>("/users/me");
  console.log("getMe response:", res.data);
  return res.data;
};

export const logoutApi = async (): Promise<void> => {
  await apiClient.post("/auth/logout");
};


export const kakaoLogin = async (code: string): Promise<AuthResponse> => {
  const res = await apiClient.post<AuthResponse>("/auth/kakao", { code });
  return res.data;
};


export const naverLogin = async (code: string, state: string): Promise<AuthResponse> => {
  const res = await apiClient.post<AuthResponse>("/auth/naver", { code, state });
  return res.data;
};

