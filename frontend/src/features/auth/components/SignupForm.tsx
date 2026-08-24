import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../hooks/useAuth";

export const SignupForm = () => {
  const navigate = useNavigate();
  const { signup, isLoading, error } = useAuth();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [passwordConfirm, setPasswordConfirm] = useState("");
  const [localError, setLocalError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLocalError(null);

    if (password !== passwordConfirm) {
      setLocalError("비밀번호가 일치하지 않습니다.");
      return;
    }

    try {
      await signup({ email, password, passwordConfirm });
      navigate("/chat");
    } catch {
      // error 상태는 useAuth에서 관리
    }
  };

  return (
    <form onSubmit={handleSubmit} className="w-[400px] border border-slate-200 rounded-xl p-8 space-y-4 shadow-sm">
      <div>
        <h2 className="text-xl font-bold">회원가입</h2>
        <p className="text-sm text-gray-500">가입 후 대화 기록 저장·문의가 가능합니다.</p>
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
          placeholder="영문·숫자·특수문자 8자 이상"
          required
          minLength={8}
          className="w-full mt-1 border rounded-lg px-3 py-2.5 text-sm"
        />
      </div>

      <div>
        <label className="text-sm font-medium">비밀번호 확인</label>
        <input
          type="password"
          value={passwordConfirm}
          onChange={(e) => setPasswordConfirm(e.target.value)}
          placeholder="다시 입력해 주세요"
          required
          className="w-full mt-1 border rounded-lg px-3 py-2.5 text-sm"
        />
      </div>

      {(localError || error) && (
        <p className="text-sm text-red-500">{localError ?? error}</p>
      )}

      <button
        type="submit"
        disabled={isLoading}
        className="w-full py-3 rounded-lg bg-violet-600 text-white font-medium disabled:opacity-50"
      >
        {isLoading ? "가입 중..." : "동의하고 가입하기"}
      </button>
    </form>
  );
};
