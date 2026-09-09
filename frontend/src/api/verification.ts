import { apiClient } from "./client";

export type ContactType = "EMAIL" | "PHONE";

export interface VerificationResult {
  success: boolean;
  message: string;
}

// ============================================================
// 비밀번호 찾기 — POST /api/verification/password/*
// ============================================================

export const sendPasswordResetCode = async (
  contactType: ContactType,
  contactValue: string
): Promise<VerificationResult> => {
  const res = await apiClient.post<VerificationResult>("/verification/password/send-code", {
    contactType,
    contactValue,
  });
  return res.data;
};

export interface ResetPasswordPayload {
  contactType: ContactType;
  contactValue: string;
  code: string;
  newPassword: string;
}

export const resetPassword = async (payload: ResetPasswordPayload): Promise<void> => {
  await apiClient.post("/verification/password/reset", payload);
};

// ============================================================
// 아이디(이메일) 찾기 — POST /api/verification/id/*
// ============================================================

export const sendIdFindCode = async (
  contactType: ContactType,
  contactValue: string
): Promise<VerificationResult> => {
  const res = await apiClient.post<VerificationResult>("/verification/id/send-code", {
    contactType,
    contactValue,
  });
  return res.data;
};

export interface FindIdResult {
  email: string;
}

export const verifyIdFindCode = async (
  contactType: ContactType,
  contactValue: string,
  code: string
): Promise<FindIdResult> => {
  const res = await apiClient.post<FindIdResult>("/verification/id/verify-code", {
    contactType,
    contactValue,
    code,
  });
  return res.data;
};

// ============================================================
// 회원가입 전화번호 인증 — POST /api/verification/signup/*
// ============================================================

export const sendSignupCode = async (
  contactValue: string
): Promise<VerificationResult> => {
  const res = await apiClient.post<VerificationResult>("/verification/signup/send-code", {
    contactType: "PHONE",
    contactValue,
  });
  return res.data;
};

export const verifySignupCode = async (
  contactValue: string,
  code: string
): Promise<VerificationResult> => {
  const res = await apiClient.post<VerificationResult>("/verification/signup/verify-code", {
    contactType: "PHONE",
    contactValue,
    code,
  });
  return res.data;
};