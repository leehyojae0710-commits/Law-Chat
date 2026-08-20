import { Navigate } from "react-router-dom";
import { useAuthStore } from "../store/authStore";

// /admin 하위 라우트를 감싸서, 로그인 안 했거나 관리자가 아니면 로그인 페이지로 보냅니다.
export const AdminRoute = ({ children }: { children: React.ReactNode }) => {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
  const isAdmin = useAuthStore((s) => s.isAdmin);

  if (!isAuthenticated) return <Navigate to="/login" replace />;
  if (!isAdmin) return <Navigate to="/" replace />;

  return <>{children}</>;
};
