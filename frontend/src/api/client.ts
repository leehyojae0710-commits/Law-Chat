import axios from "axios";

export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_URL || "http://localhost:8080/api",
  headers: {
    "Content-Type": "application/json",
  },
  timeout: 120000, // AI 응답 생성 대기 시간 (2분)
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