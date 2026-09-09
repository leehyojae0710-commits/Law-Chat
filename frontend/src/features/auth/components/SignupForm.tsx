import { useState } from "react";
import { useNavigate } from "react-router-dom";
import axios from "axios";
import { useAuth } from "../hooks/useAuth";
import { checkEmail, checkPhone, checkNickname } from "../../../api/auth";
import { sendSignupCode, verifySignupCode } from "../../../api/verification";
import { RestoreAccountModal } from "./RestoreAccountModal";
import type { ContactType } from "../../../api/verification";

type PhoneVerifyStatus = "idle" | "checked" | "code_sent" | "verified";

export const SignupForm = () => {
  const navigate = useNavigate();
  const { signup, isLoading, error } = useAuth();
  const [nicknameCheckLoading, setNicknameCheckLoading] = useState(false);
  const [nicknameNotice, setNicknameNotice] = useState<string | null>(null);
  const [nicknameError, setNicknameError] = useState<string | null>(null);
  const [nicknameAvailable, setNicknameAvailable] = useState(false);
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [passwordConfirm, setPasswordConfirm] = useState("");
  const [nickname, setNickname] = useState("");
  const [phone, setPhone] = useState("");
  const [localError, setLocalError] = useState<string | null>(null);
  const [emailCheckLoading, setEmailCheckLoading] = useState(false);
  const [emailNotice, setEmailNotice] = useState<string | null>(null);
  const [emailAvailable, setEmailAvailable] = useState(false);
  const [restoreInfo, setRestoreInfo] = useState<{
    message: string;
    verifyBy: ContactType;
    verifyTarget: string;
  } | null>(null);

  // 전화번호 인증 관련 상태
  const [phoneVerifyStatus, setPhoneVerifyStatus] = useState<PhoneVerifyStatus>("idle");
  const [phoneCheckLoading, setPhoneCheckLoading] = useState(false);
  const [sendCodeLoading, setSendCodeLoading] = useState(false);
  const [verifyCodeLoading, setVerifyCodeLoading] = useState(false);
  const [code, setCode] = useState("");
  const [phoneNotice, setPhoneNotice] = useState<string | null>(null);
  const [phoneError, setPhoneError] = useState<string | null>(null);
  const [resendCooldown, setResendCooldown] = useState(0);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLocalError(null);

    if (password !== passwordConfirm) {
      setLocalError("비밀번호가 일치하지 않습니다.");
      return;
    }

    // 추가: 이메일 중복확인을 안 거쳤으면 먼저 확인하도록 유도
    if (!emailAvailable) {
      setLocalError("이메일 중복확인을 먼저 해주세요.");
      return;
    }

    if (!nicknameAvailable) {
      setLocalError("닉네임 중복확인을 먼저 해주세요.");
      return;
    }

    // 전화번호를 입력했는데 인증을 안 마쳤으면 서버까지 안 가고 여기서 막는다.
    if (phone && phoneVerifyStatus !== "verified") {
      setLocalError("전화번호 인증을 먼저 완료해 주세요.");
      return;
    }

    try {
      await signup({ email, nickname, password, passwordConfirm, phone });
      navigate("/");
    } catch (err) {
      if (axios.isAxiosError(err) && err.response?.data?.code === "WITHDRAWN_USER_RESTORABLE") {
        try {
          const res = await checkEmail(email);
          if (res.verifyBy && res.verifyTarget) {
            setRestoreInfo({ message: res.message, verifyBy: res.verifyBy, verifyTarget: res.verifyTarget });
          }
        } catch {
          // 조회 자체가 실패하면 그냥 아래 일반 에러 메시지로 넘어감
        }
        return;
      }

      // 인증을 마쳤다고 생각했지만(10분 경과 등) 서버가 거절한 경우 — 재인증 요구
      if (axios.isAxiosError(err) && err.response?.data?.code === "PHONE_NOT_VERIFIED") {
        setPhoneVerifyStatus("checked");
        setCode("");
        setPhoneError("전화번호 인증이 만료됐어요. 다시 인증해 주세요.");
        return;
      }
      // 그 외 에러는 useAuth의 error 상태로 표시됨
    }
  };

  const formatPhoneNumber = (value: string) => {
    const numbers = value.replace(/[^0-9]/g, "").slice(0, 11);
    if (numbers.length < 4) return numbers;
    if (numbers.length < 8) return `${numbers.slice(0, 3)}-${numbers.slice(3)}`;
    return `${numbers.slice(0, 3)}-${numbers.slice(3, 7)}-${numbers.slice(7)}`;
  };

  // 전화번호를 수정하면 인증 상태를 전부 초기화 (명세 필수 규칙)
  const handlePhoneChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setPhone(formatPhoneNumber(e.target.value));
    setPhoneVerifyStatus("idle");
    setCode("");
    setPhoneNotice(null);
    setPhoneError(null);
  };

  // 이메일을 수정하면 확인 상태 초기화
  const handleEmailChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setEmail(e.target.value);
    setEmailAvailable(false);
    setEmailNotice(null);
  };

  // 이메일 [중복확인] 버튼
  const handleEmailCheck = async () => {
    if (!email) return;
    setEmailCheckLoading(true);
    setLocalError(null);
    try {
      const res = await checkEmail(email);
      if (res.status === "WITHDRAWN" && res.restorable && res.verifyBy && res.verifyTarget) {
        setRestoreInfo({ message: res.message, verifyBy: res.verifyBy, verifyTarget: res.verifyTarget });
        return;
      }
      if (res.status === "AVAILABLE") {
        setEmailAvailable(true);
        setEmailNotice(res.message);
      } else {
        setEmailAvailable(false);
        setLocalError(res.message);
      }
    } catch {
      setLocalError("중복확인에 실패했습니다. 다시 시도해 주세요.");
    } finally {
      setEmailCheckLoading(false);
    }
  };

    // 닉네임을 수정하면 확인 상태 초기화
  const handleNicknameChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setNickname(e.target.value);
    setNicknameAvailable(false);
    setNicknameNotice(null);
    setNicknameError(null);
  };

  // 닉네임 [중복확인] 버튼
  const handleNicknameCheck = async () => {
    if (!nickname) return;
    setNicknameCheckLoading(true);
    setNicknameError(null);
    try {
      const res = await checkNickname(nickname);
      if (res.available) {
        setNicknameAvailable(true);
        setNicknameNotice("사용할 수 있는 닉네임이에요.");
      } else {
        setNicknameAvailable(false);
        setNicknameError("이미 사용 중인 닉네임이에요.");
      }
    } catch {
      setNicknameError("중복확인에 실패했습니다. 다시 시도해 주세요.");
    } finally {
      setNicknameCheckLoading(false);
    }
  };

  // 전화번호 [중복확인] 버튼
  const handlePhoneCheck = async () => {
    if (!phone) return;
    setPhoneCheckLoading(true);
    setPhoneError(null);
    try {
      const res = await checkPhone(phone);
      if (res.status === "WITHDRAWN" && res.restorable && res.verifyBy && res.verifyTarget) {
        setRestoreInfo({ message: res.message, verifyBy: res.verifyBy, verifyTarget: res.verifyTarget });
        return;
      }
      if (res.status === "AVAILABLE") {
        setPhoneVerifyStatus("checked"); // [인증하기] 버튼 활성화
        setPhoneNotice(res.message);
      } else {
        setPhoneVerifyStatus("idle");
        setPhoneError(res.message);
      }
    } catch {
      setPhoneError("중복확인에 실패했습니다. 다시 시도해 주세요.");
    } finally {
      setPhoneCheckLoading(false);
    }
  };

  // [인증하기] 버튼 — 코드 발송 + 재클릭 방지 쿨다운
  const handleSendCode = async () => {
    if (!phone || resendCooldown > 0) return;
    setSendCodeLoading(true);
    setPhoneError(null);
    try {
      const res = await sendSignupCode(phone);
      setPhoneNotice(res.message);
      setPhoneVerifyStatus("code_sent");
      setResendCooldown(60);
      const timer = setInterval(() => {
        setResendCooldown((prev) => {
          if (prev <= 1) {
            clearInterval(timer);
            return 0;
          }
          return prev - 1;
        });
      }, 1000);
    } catch {
      setPhoneError("인증코드 발송에 실패했습니다. 다시 시도해 주세요.");
    } finally {
      setSendCodeLoading(false);
    }
  };

  const handleCodeChange = (v: string) => setCode(v.replace(/\D/g, "").slice(0, 6));

  // 코드 [확인] 버튼
  const handleVerifyCode = async () => {
    if (!/^\d{6}$/.test(code)) {
      setPhoneError("인증코드는 숫자 6자리입니다.");
      return;
    }
    setVerifyCodeLoading(true);
    setPhoneError(null);
    try {
      const res = await verifySignupCode(phone, code);
      setPhoneNotice(res.message);
      setPhoneVerifyStatus("verified");
    } catch (err) {
      if (axios.isAxiosError(err) && err.response?.data?.message) {
        setPhoneError(err.response.data.message);
      } else {
        setPhoneError("인증에 실패했습니다. 다시 시도해 주세요.");
      }
    } finally {
      setVerifyCodeLoading(false);
    }
  };

  return (
    <>
      <form onSubmit={handleSubmit} className="w-[400px] border border-slate-200 rounded-xl p-8 space-y-4 shadow-sm">
        <div>
          <h2 className="text-xl font-bold">회원가입</h2>
          <p className="text-sm text-gray-500">가입 후 대화 기록 저장·문의가 가능합니다.</p>
        </div>

        <div>
          <label className="text-sm font-medium">이메일</label>
          <div className="flex gap-2 mt-1">
            <input
              type="email"
              value={email}
              onChange={handleEmailChange}
              placeholder="you@example.com"
              required
              className="flex-1 border rounded-lg px-3 py-2.5 text-sm"
            />
            <button
              type="button"
              onClick={handleEmailCheck}
              disabled={!email || emailCheckLoading}
              className="shrink-0 px-3 py-2.5 rounded-lg border text-sm font-medium disabled:opacity-50"
            >
              {emailCheckLoading ? "확인 중..." : "중복확인"}
            </button>
          </div>
          {emailAvailable && emailNotice && (
            <p className="text-xs text-green-600 mt-1 font-medium">✓ {emailNotice}</p>
          )}
        </div>

        <div>
          <label className="text-sm font-medium">닉네임</label>
          <div className="flex gap-2 mt-1">
            <input
              type="text"
              value={nickname}
              onChange={handleNicknameChange}
              placeholder="닉네임"
              required
              className="flex-1 border rounded-lg px-3 py-2.5 text-sm"
            />
            <button
              type="button"
              onClick={handleNicknameCheck}
              disabled={!nickname || nicknameCheckLoading}
              className="shrink-0 px-3 py-2.5 rounded-lg border text-sm font-medium disabled:opacity-50"
            >
              {nicknameCheckLoading ? "확인 중..." : "중복확인"}
            </button>
          </div>
          {nicknameAvailable && nicknameNotice && (
            <p className="text-xs text-green-600 mt-1 font-medium">✓ {nicknameNotice}</p>
          )}
          {nicknameError && <p className="text-xs text-red-500 mt-1">{nicknameError}</p>}
        </div>

        <div>
          <label className="text-sm font-medium">전화번호 (선택)</label>
          <div className="flex gap-2 mt-1">
            <input
              type="tel"
              value={phone}
              onChange={handlePhoneChange}
              placeholder="010-1234-5678"
              pattern="01[0-9]-[0-9]{3,4}-[0-9]{4}"
              title="올바른 휴대폰 번호 형식이 아닙니다. (예: 010-1234-5678)"
              className="flex-1 border rounded-lg px-3 py-2.5 text-sm"
            />
            <button
              type="button"
              onClick={handlePhoneCheck}
              disabled={!phone || phoneCheckLoading}
              className="shrink-0 px-3 py-2.5 rounded-lg border text-sm font-medium disabled:opacity-50"
            >
              {phoneCheckLoading ? "확인 중..." : "중복확인"}
            </button>
            <button
              type="button"
              onClick={handleSendCode}
              disabled={phoneVerifyStatus === "idle" || phoneVerifyStatus === "verified" || sendCodeLoading || resendCooldown > 0}
              className="shrink-0 px-3 py-2.5 rounded-lg bg-violet-600 text-white text-sm font-medium disabled:opacity-50"
            >
              {resendCooldown > 0
                ? `재발송 ${resendCooldown}s`
                : sendCodeLoading
                  ? "발송 중..."
                  : phoneVerifyStatus === "verified"
                    ? "인증완료"
                    : "인증하기"}
            </button>
          </div>

          {phone && phoneVerifyStatus !== "idle" && phoneVerifyStatus !== "verified" && (
            <div className="flex gap-2 mt-2">
              <input
                value={code}
                onChange={(e) => handleCodeChange(e.target.value)}
                placeholder="인증코드 6자리"
                maxLength={6}
                inputMode="numeric"
                className="flex-1 border rounded-lg px-3 py-2 text-sm"
              />
              <button
                type="button"
                onClick={handleVerifyCode}
                disabled={verifyCodeLoading}
                className="shrink-0 px-3 py-2 rounded-lg bg-violet-600 text-white text-sm font-medium disabled:opacity-50"
              >
                {verifyCodeLoading ? "확인 중..." : "확인"}
              </button>
            </div>
          )}

          {phoneVerifyStatus === "verified" && (
            <p className="text-xs text-green-600 mt-1 font-medium">✓ 인증완료</p>
          )}
          {phoneNotice && phoneVerifyStatus !== "verified" && (
            <p className="text-xs text-gray-500 mt-1">{phoneNotice}</p>
          )}
          {phoneError && <p className="text-xs text-red-500 mt-1">{phoneError}</p>}

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
          disabled={isLoading || !emailAvailable || !nicknameAvailable || (!!phone && phoneVerifyStatus !== "verified")}
          className="w-full py-3 rounded-lg bg-violet-600 text-white font-medium disabled:opacity-50"
        >
          {isLoading ? "가입 중..." : "동의하고 가입하기"}
        </button>
      </form>

      {restoreInfo && (
        <RestoreAccountModal
          message={restoreInfo.message}
          verifyBy={restoreInfo.verifyBy}
          verifyTarget={restoreInfo.verifyTarget}
          onClose={() => setRestoreInfo(null)}
        />
      )}
    </>
  );
};