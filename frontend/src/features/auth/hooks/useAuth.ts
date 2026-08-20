import { useState } from "react";
import { login as loginApi, signup as signupApi, logoutApi } from "../../../api/auth";
import { useAuthStore } from "../../../store/authStore";
import type { LoginPayload, SignupPayload } from "../types";

export const useAuth = () => {
  const setAuth = useAuthStore((s) => s.setAuth);
  const logoutStore = useAuthStore((s) => s.logout);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const login = async (payload: LoginPayload) => {
    setIsLoading(true);
    setError(null);
    try {
      const { user, accessToken } = await loginApi(payload);
      setAuth(user, accessToken);
      return user;
    } catch (err) {
      setError("이메일 또는 비밀번호가 올바르지 않습니다.");
      throw err;
    } finally {
      setIsLoading(false);
    }
  };

  const signup = async (payload: SignupPayload) => {
    setIsLoading(true);
    setError(null);
    try {
      const { user, accessToken } = await signupApi(payload);
      setAuth(user, accessToken);
      return user;
    } catch (err) {
      setError("회원가입에 실패했습니다. 입력값을 확인해주세요.");
      throw err;
    } finally {
      setIsLoading(false);
    }
  };

  const logout = async () => {
    try {
      await logoutApi();
    } finally {
      logoutStore();
    }
  };

  return { login, signup, logout, isLoading, error };
};
