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
const storedToken = localStorage.getItem("accessToken");
const storedUserRaw = localStorage.getItem("authUser");
const storedUser: AuthUser | null = storedUserRaw ? JSON.parse(storedUserRaw) : null;

export const useAuthStore = create<AuthState>((set) => ({
  user: storedUser,
  accessToken: storedToken,
  isAuthenticated: !!storedToken,
  isAdmin: storedUser?.role === "ADMIN",

  setAuth: (user, accessToken) => {
    localStorage.setItem("accessToken", accessToken);
    localStorage.setItem("authUser", JSON.stringify(user));
    set({ user, accessToken, isAuthenticated: true, isAdmin: user.role === "ADMIN" });
  },

  logout: () => {
    localStorage.removeItem("accessToken");
    localStorage.removeItem("authUser");
    set({ user: null, accessToken: null, isAuthenticated: false, isAdmin: false });
  },
}));
