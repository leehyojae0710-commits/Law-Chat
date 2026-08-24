import { useEffect, useRef, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { useAuth } from "../features/auth/hooks/useAuth";

export const NaverCallbackPage = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { naverLogin } = useAuth();
  const [error, setError] = useState<string | null>(null);
  const calledRef = useRef(false);

  useEffect(() => {
    if (calledRef.current) return;
    calledRef.current = true;

    const code = searchParams.get("code");
    if (!code) {
      setError("네이버 로그인이 취소되었거나 실패했습니다.");
      return;
    }

    naverLogin(code)
      .then(() => navigate("/", { replace: true }))
      .catch(() => setError("네이버 로그인 처리 중 오류가 발생했습니다."));
  }, [searchParams, naverLogin, navigate]);

  return (
    <div className="min-h-screen flex items-center justify-center">
      <p className="text-sm text-slate-500">
        {error ?? "네이버 로그인 처리 중입니다..."}
      </p>
    </div>
  );
};