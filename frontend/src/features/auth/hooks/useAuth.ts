import { useState } from "react";
import { login as loginApi, signup as signupApi, logoutApi, kakaoLogin as kakaoLoginApi, naverLogin as NaverLoginApi} from "../../../api/auth";
import { mockLogin, mockSignup, mockLogout } from "../mockAuth";
import { useAuthStore } from "../../../store/authStore";
import type { LoginPayload, SignupPayload } from "../types";

// 백엔드가 준비되기 전에는 .env의 VITE_USE_MOCK_AUTH=true 로 mock 데이터를 사용합니다.
// 백엔드 연동 시 .env에서 VITE_USE_MOCK_AUTH=false 로만 바꾸면 실제 API를 호출합니다.
const USE_MOCK_AUTH = import.meta.env.VITE_USE_MOCK_AUTH === "true";

export const useAuth = () => {
  const setAuth = useAuthStore((s) => s.setAuth);
  const logoutStore = useAuthStore((s) => s.logout);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const login = async (payload: LoginPayload) => {
    setIsLoading(true);
    setError(null);
    try {
      const { user, accessToken } = USE_MOCK_AUTH
        ? await mockLogin(payload)
        : await loginApi(payload);
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
      const { user, accessToken } = USE_MOCK_AUTH
        ? await mockSignup(payload)
        : await signupApi(payload);
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
      USE_MOCK_AUTH ? await mockLogout() : await logoutApi();
    } finally {
      logoutStore();
    }
  };
  
  const kakaoLogin = async (code: string) => {
    const { user, accessToken } = await kakaoLoginApi(code); // 백엔드 /auth/kakao 호출
    setAuth(user, accessToken); // 여기서 로그인 상태(localStorage + zustand) 세팅됨
    return user;
  }

  const naverLogin = async (code: string) => {
    const { user, accessToken } = await NaverLoginApi(code); // 백엔드 /auth/kakao 호출
    setAuth(user, accessToken); // 여기서 로그인 상태(localStorage + zustand) 세팅됨
    return user;
  }

  return { login, signup, logout, kakaoLogin, naverLogin, isLoading, error };
};
