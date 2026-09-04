import { useEffect, useState } from "react";

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

export const useDarkMode = () => {
  const [theme, setTheme] = useState<Theme>(getInitialTheme);

  useEffect(() => {
    const root = document.documentElement;
    if (theme === "dark") {
      root.classList.add("dark");
    } else {
      root.classList.remove("dark");
    }
    localStorage.setItem(STORAGE_KEY, theme);
  }, [theme]);

  const toggleTheme = () => {
    setTheme((prev) => (prev === "dark" ? "light" : "dark"));
  };

  return { theme, isDark: theme === "dark", toggleTheme };
};