import { create } from "zustand";
import type { AuthUser } from "../features/auth/types";

interface AuthState {
  user: AuthUser | null;
  accessToken: string | null;
  isAuthenticated: boolean;
  isAdmin: boolean;
  setAuth: (user: AuthUser, accessToken: string) => void;
  logout: () => void;
}

// 새로고침해도 로그인 유지되도록 localStorage에서 초기값을 읽어옵니다.
const storedToken = sessionStorage.getItem("accessToken");
const storedUserRaw = sessionStorage.getItem("authUser");
const storedUser: AuthUser | null = storedUserRaw ? JSON.parse(storedUserRaw) : null;

export const useAuthStore = create<AuthState>((set) => ({
  user: storedUser,
  accessToken: storedToken,
  isAuthenticated: !!storedToken,
  isAdmin: storedUser?.isAdmin ?? false,
  

  setAuth: (user, accessToken) => {
    sessionStorage.setItem("accessToken", accessToken);
    sessionStorage.setItem("authUser", JSON.stringify(user));
    set({ user, accessToken, isAuthenticated: true, isAdmin: user.isAdmin });
  },

  logout: () => {
    sessionStorage.removeItem("accessToken");
    sessionStorage.removeItem("authUser");
    set({ user: null, accessToken: null, isAuthenticated: false, isAdmin: false });
  },
}));
