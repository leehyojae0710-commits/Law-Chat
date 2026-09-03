import { useEffect } from "react";
import { AppRoutes } from "./routes";
import axios from "axios";

function App() {
  useEffect(() => {
    const refreshToken = () => {
      axios.get('/api/auth/verify', {
        headers: { Authorization: `Bearer ${sessionStorage.getItem("accessToken")}` }
      })
      .then(res => console.log('토큰 검증 성공', res.data))
      .catch(err => {
        console.error('토큰 검증 실패', err);
        // 토큰 검증 실패 시 로그아웃 처리
        sessionStorage.removeItem("accessToken");
        window.location.href = "/login"; // 로그인 페이지로 리다이렉트
      });
    };

    const interval = setInterval(refreshToken, 1 * 60 * 1000);
    return () => clearInterval(interval);
  }, []);
  
  return <AppRoutes />;
}

export default App;
