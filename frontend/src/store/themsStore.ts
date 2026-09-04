import { create } from "zustand";

const STORAGE_KEY = "theme";

type Theme = "light" | "dark";

const getInitialTheme = (): Theme => {
  if (typeof window === "undefined") return "light";

  const saved = localStorage.getItem(STORAGE_KEY);
  if (saved === "dark" || saved === "light") return saved;

  // 저장된 값이 없으면 OS 설정을 기본값으로 사용
  return window.matchMedia("(prefers-color-scheme: dark)").matches
    ? "dark"
    : "light";
};

const applyTheme = (theme: Theme) => {
  const root = document.documentElement;
  if (theme === "dark") {
    root.classList.add("dark");
  } else {
    root.classList.remove("dark");
  }
  localStorage.setItem(STORAGE_KEY, theme);
};

interface ThemeState {
  theme: Theme;
  isDark: boolean;
  toggleTheme: () => void;
}

// 모듈이 로드되는 즉시(=앱이 켜지는 즉시) 저장된 테마를 적용합니다.
// 설정 모달을 열지 않아도, 새로고침만 해도 바로 반영돼야 하기 때문에
// 컴포넌트의 useEffect가 아니라 여기서 한 번 실행합니다.
const initialTheme = getInitialTheme();
applyTheme(initialTheme);

export const useThemeStore = create<ThemeState>((set, get) => ({
  theme: initialTheme,
  isDark: initialTheme === "dark",
  toggleTheme: () => {
    const next: Theme = get().theme === "dark" ? "light" : "dark";
    applyTheme(next);
    set({ theme: next, isDark: next === "dark" });
  },
}));