import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../hooks/useAuth";

export const SignupForm = () => {
  const navigate = useNavigate();
  const { signup, isLoading, error } = useAuth();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [passwordConfirm, setPasswordConfirm] = useState("");
  const [nickname, setNickname] = useState("");
  const [phone, setPhone] = useState("");
  const [localError, setLocalError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLocalError(null);

    if (password !== passwordConfirm) {
      setLocalError("비밀번호가 일치하지 않습니다.");
      return;
    }

    try {
      // 백엔드가 하이픈 유무 상관없이 받아 숫자만 정규화하므로 프론트에서 따로 가공하지 않고 그대로 보낸다.
      await signup({ email, nickname, password, passwordConfirm, phone });
      navigate("/");
    } catch {
      // error 상태는 useAuth에서 관리
    }
  };
  const formatPhoneNumber = (value: string) => {
    // 숫자만 남기기
    const numbers = value.replace(/[^0-9]/g, "").slice(0, 11);

    if (numbers.length < 4) {
      return numbers;
    } else if (numbers.length < 8) {
      return `${numbers.slice(0, 3)}-${numbers.slice(3)}`;
    } else {
      return `${numbers.slice(0, 3)}-${numbers.slice(3, 7)}-${numbers.slice(7)}`;
    }
  };

  const handlePhoneChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setPhone(formatPhoneNumber(e.target.value));
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
        <label className="text-sm font-medium">닉네임</label>
        <input
          type="nickname"
          value={nickname}
          onChange={(e) => setNickname(e.target.value)}
          placeholder="닉네임"
          required
          className="w-full mt-1 border rounded-lg px-3 py-2.5 text-sm"
        />
      </div>

      <div>
        <label className="text-sm font-medium">전화번호</label>
        <input
          type="tel"
          value={phone}
          onChange={handlePhoneChange}
          placeholder="010-1234-5678"
          required
          pattern="01[0-9]-[0-9]{3,4}-[0-9]{4}"
          title="올바른 휴대폰 번호 형식이 아닙니다. (예: 010-1234-5678)"
          className="w-full mt-1 border rounded-lg px-3 py-2.5 text-sm"
        />
        <p className="text-xs text-gray-400 mt-1">아이디 찾기·비밀번호 재설정에 사용돼요.</p>
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
