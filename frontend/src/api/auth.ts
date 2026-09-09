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

export const updateProfile = async(data : {nickname?:string , phone?:string}): Promise<AuthUser_Profile> => {
  console.log("updateProfile payload:", data);
  const res = await apiClient.patch<AuthUser_Profile>("/users/me", data);
  console.log("updateProfile response:", res.data);
  return res.data;
}

export const updateProfileImg = async (file: File): Promise<AuthUser_Profile> => {
  const formData = new FormData();
  formData.append("file", file);
  const res = await apiClient.post<AuthUser_Profile>("/users/me/profile-image", formData, {
    headers: {
      "Content-Type": "multipart/form-data",
    },
  });
  return res.data;
}

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


export const deleteUserAccount = async (): Promise<void> => {
  await apiClient.delete("/users/me");
}

export interface CheckAvailabilityResult {
  available: boolean;
  status: "AVAILABLE" | "IN_USE" | "WITHDRAWN";
  deletedAt: string | null;
  deletedAtText: string | null;
  maskedEmail: string | null;
  restorable: boolean;
  verifyBy: "EMAIL" | "PHONE" | null;
  verifyTarget: string | null;
  message: string;
}

export const checkEmail = async (email: string): Promise<CheckAvailabilityResult> => {
  const res = await apiClient.get<CheckAvailabilityResult>("/users/check-email", {
    params: { email },
  });
  return res.data;
};

export const checkPhone = async (phone: string): Promise<CheckAvailabilityResult> => {
  const res = await apiClient.get<CheckAvailabilityResult>("/users/check-phone", {
    params: { phone },
  });
  return res.data;
};

export const restoreAccount = async (contactValue: string): Promise<void> => {
  await apiClient.post("/auth/restore", { contactValue });
};

export const checkNickname = async (nickname: string): Promise<{ available: boolean }> => {
  const res = await apiClient.get<{ available: boolean }>("/users/check-nickname", {
    params: { nickname },
  });
  return res.data;
};