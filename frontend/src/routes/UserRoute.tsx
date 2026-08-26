import { Navigate } from "react-router-dom";
import { useAuthStore } from "../store/authStore";

// /chat 하위 라우트를 감싸서, 로그인하지 않았으면 로그인 페이지로 보냅니다.
export const UserRoute = ({ children }: { children: React.ReactNode }) => {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);

  if (!isAuthenticated) return <Navigate to="/login" replace />;

  return <>{children}</>;
};
