import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../hooks/useAuth";
import { KakaoLoginButton } from "../components/kakaoLogin"
import { NaverLoginButton } from "../components/naverLogin"

export const LoginForm = () => {
  const navigate = useNavigate();
  const { login, isLoading, error } = useAuth();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await login({ email, password });
      navigate("/");
    } catch {
      // 에러는 useAuth의 error 상태로 화면에 표시됨
      console.error("로그인 실패");
    }
  };

  return (
    <form onSubmit={handleSubmit} className="w-full border border-slate-200 rounded-xl p-8 space-y-4 shadow-sm">
      <div>
        <h2 className="text-xl font-bold">로그인</h2>
        <p className="text-sm text-gray-500">상담 기록과 요약서를 이용하려면 로그인이 필요합니다.</p>
      </div>

      <div>
        <label className="text-sm font-medium">이메일</label>
        <input
          type="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          placeholder="you@example.com"
          required
          className="w-full mt-1 border rounded-lg px-3 py-2.5 text-sm"
        />
      </div>

      <div>
        <label className="text-sm font-medium">비밀번호</label>
        <input
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          placeholder="••••••••"
          required
          className="w-full mt-1 border rounded-lg px-3 py-2.5 text-sm"
        />
      </div>

      {error && <p className="text-sm text-red-500">{error}</p>}

      <button
        type="submit"
        disabled={isLoading}
        className="w-full py-3 rounded-lg bg-violet-600 text-white font-medium disabled:opacity-50"
      >
        {isLoading ? "로그인 중..." : "로그인"}
      </button>

      <button
        type="button"
        onClick={()=>navigate("/Signup")}
        className="w-full py-3 rounded-lg bg-violet-600 text-white font-medium disabled:opacity-50"
      >
        회원가입
      </button>

      <div className="flex flex-col gap-3">
        <KakaoLoginButton />
        <NaverLoginButton />
      </div>
    </form>
  );
};
