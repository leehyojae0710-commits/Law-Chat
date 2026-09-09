import axios from "axios";

export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_URL || "http://localhost:8080/api",
  headers: {
    "Content-Type": "application/json",
  },
  timeout: 120000, // AI 응답 생성 대기 시간 (2분)
  // 배열 파라미터를 "category=A&category=B" 형식(같은 이름 반복)으로 직렬화.
  // axios 기본값은 "category[]=A&category[]=B"인데, Spring의 @RequestParam List<String>은
  // 이 대괄호 형식을 못 읽는다 (PrecedentController#search 참고). indexes: null이 반복 형식.
  paramsSerializer: {
    indexes: null,
  },
});

// 인증 없이 호출해야 하는 공개 API 경로 — 여기 해당하면 토큰을 붙이지 않는다.
// (탈퇴 계정 복구처럼 옛날 토큰이 남아있는 상태에서 호출되는 API가 있어서,
//  무조건 토큰을 붙이면 서버가 "탈퇴 회원의 토큰"으로 401을 내려버린다.)
const PUBLIC_PATHS = [
  "/auth/login",
  "/auth/signup",
  "/auth/restore",
  "/auth/kakao",
  "/auth/naver",
  "/verification/",
  "/users/check-email",
  "/users/check-phone",
];

apiClient.interceptors.request.use(
  (config) => {
    const isPublic = PUBLIC_PATHS.some((p) => config.url?.includes(p));

    if (config.data instanceof FormData) {
      delete config.headers["Content-Type"];
    }

    if (!isPublic) {
      // sessionStorage와 localStorage 양쪽에서 토큰을 모두 탐색
      const token =
        sessionStorage.getItem("accessToken") ||
        localStorage.getItem("accessToken") ||
        sessionStorage.getItem("token") ||
        localStorage.getItem("token");

      if (token && config.headers) {
        config.headers.Authorization = `Bearer ${token}`;
      }
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

export default apiClient;