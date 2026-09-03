import { useEffect } from "react";
import { AppRoutes } from "./routes";
import { apiClient } from "./api/client";

function App() {
  useEffect(() => {
    const token = sessionStorage.getItem('accessToken')
    if (!token) return;
    const refreshToken = () => {
      apiClient.get('/auth/verify')
        .then(res => console.log('토큰 검증 성공', res.data))
        .catch(err => {
          console.error('토큰 검증 실패', err);
          sessionStorage.removeItem("accessToken");
          window.location.href = "/login";
        });
    };

    const interval = setInterval(refreshToken, 1 * 60 * 1000);
    return () => clearInterval(interval);
  }, []);

  return <AppRoutes />;
}

export default App;
