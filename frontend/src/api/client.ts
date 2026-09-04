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

apiClient.interceptors.request.use(
  (config) => {
    // sessionStorage와 localStorage 양쪽에서 토큰을 모두 탐색
    const token =
      sessionStorage.getItem("accessToken") ||
      localStorage.getItem("accessToken") ||
      sessionStorage.getItem("token") ||
      localStorage.getItem("token");

    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

export default apiClient;