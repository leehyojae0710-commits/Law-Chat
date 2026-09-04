import { useState } from "react";
import axios from "axios";
import {
  sendPasswordResetCode,
  resetPassword,
  sendIdFindCode,
  verifyIdFindCode,
  type ContactType,
} from "../../../api/verification";

const PASSWORD_PATTERN = /^(?=.*[A-Za-z])(?=.*\d).{8,64}$/;

const extractErrorMessage = (err: unknown, fallback: string): string => {
  if (axios.isAxiosError(err) && err.response?.data && typeof err.response.data === "object") {
    const data = err.response.data as { message?: string };
    if (data.message) return data.message;
  }
  return fallback;
};

// EMAIL/PHONE 전환 버튼 — 두 패널에서 공통으로 씀
const ContactTypeToggle = ({
  value,
  onChange,
}: {
  value: ContactType;
  onChange: (v: ContactType) => void;
}) => (
  <div className="flex gap-2 text-sm">
    <button
      type="button"
      onClick={() => onChange("EMAIL")}
      className={`px-3 py-1.5 rounded-full border ${value === "EMAIL" ? "bg-purple-600 text-white border-purple-600" : "text-gray-600"}`}
    >
      이메일
    </button>
    <button
      type="button"
      onClick={() => onChange("PHONE")}
      className={`px-3 py-1.5 rounded-full border ${value === "PHONE" ? "bg-purple-600 text-white border-purple-600" : "text-gray-600"}`}
    >
      휴대폰(SMS)
    </button>
  </div>
);

const contactLabel = (type: ContactType) => (type === "EMAIL" ? "가입한 이메일" : "가입한 휴대폰 번호");
const contactPlaceholder = (type: ContactType) =>
  type === "EMAIL" ? "example@email.com" : "01012345678 (숫자만)";

// ============================================================
// 아이디(이메일) 찾기
// ============================================================

type IdFindStep = "input" | "verify" | "done";

const IdFindPanel = () => {
  const [contactType, setContactType] = useState<ContactType>("EMAIL");
  const [contactValue, setContactValue] = useState("");
  const [code, setCode] = useState("");
  const [step, setStep] = useState<IdFindStep>("input");
  const [foundEmail, setFoundEmail] = useState("");

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  const handleContactTypeChange = (v: ContactType) => {
    setContactType(v);
    setContactValue("");
    setError(null);
  };

  const handleSendCode = async () => {
    const trimmed = contactValue.trim();
    if (!trimmed) {
      setError(contactType === "EMAIL" ? "이메일을 입력해 주세요." : "휴대폰 번호를 입력해 주세요.");
      return;
    }

    setError(null);
    setIsSubmitting(true);
    try {
      const result = await sendIdFindCode(contactType, trimmed);
      setNotice(result.message);
      setStep("verify");
    } catch (err) {
      console.error("아이디 찾기 인증코드 발송 실패:", err);
      setError(extractErrorMessage(err, "인증코드 발송에 실패했습니다. 다시 시도해 주세요."));
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleVerify = async () => {
    if (!/^\d{6}$/.test(code)) {
      setError("인증코드는 숫자 6자리입니다.");
      return;
    }

    setError(null);
    setIsSubmitting(true);
    try {
      const result = await verifyIdFindCode(contactType, contactValue.trim(), code);
      setFoundEmail(result.email);
      setStep("done");
      setNotice(null);
    } catch (err) {
      console.error("아이디 찾기 인증 실패:", err);
      setError(extractErrorMessage(err, "인증에 실패했습니다. 다시 시도해 주세요."));
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleReset = () => {
    setStep("input");
    setContactValue("");
    setCode("");
    setError(null);
    setNotice(null);
  };

  if (step === "done") {
    return (
      <div className="space-y-3 text-center py-6">
        <p className="text-sm text-gray-500">회원님의 아이디(이메일)는</p>
        <p className="font-semibold text-lg">{foundEmail}</p>
        <p className="text-sm text-gray-500">입니다.</p>
      </div>
    );
  }

  return (
    <div className="space-y-3">
      <ContactTypeToggle value={contactType} onChange={handleContactTypeChange} />

      {step === "input" && (
        <>
          <div>
            <p className="text-sm font-medium mb-1">{contactLabel(contactType)}</p>
            <input
              value={contactValue}
              onChange={(e) => setContactValue(e.target.value)}
              placeholder={contactPlaceholder(contactType)}
              inputMode={contactType === "PHONE" ? "numeric" : "email"}
              className="w-full border rounded-lg px-3 py-2 text-sm"
            />
          </div>
          {error && <p className="text-xs text-red-500">{error}</p>}
          <button
            onClick={handleSendCode}
            disabled={isSubmitting}
            className="px-4 py-2 rounded-lg bg-purple-600 text-white text-sm font-medium disabled:opacity-50"
          >
            {isSubmitting ? "발송 중..." : "인증코드 받기"}
          </button>
        </>
      )}

      {step === "verify" && (
        <>
          {notice && <p className="text-xs text-gray-500">{notice}</p>}
          <div>
            <p className="text-sm font-medium mb-1">인증코드</p>
            <input
              value={code}
              onChange={(e) => setCode(e.target.value.replace(/\D/g, "").slice(0, 6))}
              placeholder="숫자 6자리"
              maxLength={6}
              inputMode="numeric"
              className="w-full border rounded-lg px-3 py-2 text-sm"
            />
          </div>
          {error && <p className="text-xs text-red-500">{error}</p>}
          <div className="flex gap-2">
            <button
              onClick={handleReset}
              disabled={isSubmitting}
              className="px-4 py-2 rounded-lg border text-sm font-medium disabled:opacity-50"
            >
              다시 입력
            </button>
            <button
              onClick={handleVerify}
              disabled={isSubmitting}
              className="px-4 py-2 rounded-lg bg-purple-600 text-white text-sm font-medium disabled:opacity-50"
            >
              {isSubmitting ? "확인 중..." : "확인"}
            </button>
          </div>
        </>
      )}
    </div>
  );
};

// ============================================================
// 비밀번호 찾기
// ============================================================

type PasswordStep = "input" | "reset" | "done";

const PasswordFindPanel = () => {
  const [contactType, setContactType] = useState<ContactType>("EMAIL");
  const [contactValue, setContactValue] = useState("");
  const [code, setCode] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [step, setStep] = useState<PasswordStep>("input");

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  const handleContactTypeChange = (v: ContactType) => {
    setContactType(v);
    setContactValue("");
    setError(null);
  };

  const handleSendCode = async () => {
    const trimmed = contactValue.trim();
    if (!trimmed) {
      setError(contactType === "EMAIL" ? "이메일을 입력해 주세요." : "휴대폰 번호를 입력해 주세요.");
      return;
    }

    setError(null);
    setIsSubmitting(true);
    try {
      const result = await sendPasswordResetCode(contactType, trimmed);
      // 가입 여부와 무관하게 항상 success: true가 내려온다 (계정 열거 공격 방지).
      setNotice(result.message);
      setStep("reset");
    } catch (err) {
      console.error("인증코드 발송 실패:", err);
      setError(extractErrorMessage(err, "인증코드 발송에 실패했습니다. 다시 시도해 주세요."));
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleReset = async () => {
    if (!/^\d{6}$/.test(code)) {
      setError("인증코드는 숫자 6자리입니다.");
      return;
    }
    if (!PASSWORD_PATTERN.test(newPassword)) {
      setError("비밀번호는 영문과 숫자를 모두 포함해 8~64자로 입력해 주세요.");
      return;
    }
    if (newPassword !== confirmPassword) {
      setError("새 비밀번호가 일치하지 않습니다.");
      return;
    }

    setError(null);
    setIsSubmitting(true);
    try {
      await resetPassword({ contactType, contactValue: contactValue.trim(), code, newPassword });
      setStep("done");
      setNotice(null);
    } catch (err) {
      console.error("비밀번호 재설정 실패:", err);
      setError(extractErrorMessage(err, "비밀번호 재설정에 실패했습니다. 다시 시도해 주세요."));
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleBack = () => {
    setStep("input");
    setCode("");
    setNewPassword("");
    setConfirmPassword("");
    setError(null);
    setNotice(null);
  };

  if (step === "done") {
    return (
      <div className="space-y-3 text-center py-6">
        <p className="font-semibold">비밀번호가 변경되었습니다</p>
        <p className="text-sm text-gray-500">새 비밀번호로 로그인해 주세요.</p>
      </div>
    );
  }

  return (
    <div className="space-y-3">
      <ContactTypeToggle value={contactType} onChange={handleContactTypeChange} />

      {step === "input" && (
        <>
          <div>
            <p className="text-sm font-medium mb-1">{contactLabel(contactType)}</p>
            <input
              value={contactValue}
              onChange={(e) => setContactValue(e.target.value)}
              placeholder={contactPlaceholder(contactType)}
              inputMode={contactType === "PHONE" ? "numeric" : "email"}
              className="w-full border rounded-lg px-3 py-2 text-sm"
            />
          </div>
          {error && <p className="text-xs text-red-500">{error}</p>}
          <button
            onClick={handleSendCode}
            disabled={isSubmitting}
            className="px-4 py-2 rounded-lg bg-purple-600 text-white text-sm font-medium disabled:opacity-50"
          >
            {isSubmitting ? "발송 중..." : "인증코드 받기"}
          </button>
        </>
      )}

      {step === "reset" && (
        <>
          {notice && <p className="text-xs text-gray-500">{notice}</p>}
          <div>
            <p className="text-sm font-medium mb-1">인증코드</p>
            <input
              value={code}
              onChange={(e) => setCode(e.target.value.replace(/\D/g, "").slice(0, 6))}
              placeholder="숫자 6자리"
              maxLength={6}
              inputMode="numeric"
              className="w-full border rounded-lg px-3 py-2 text-sm"
            />
          </div>
          <div>
            <p className="text-sm font-medium mb-1">새 비밀번호</p>
            <input
              type="password"
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              placeholder="영문, 숫자 포함 8~64자"
              className="w-full border rounded-lg px-3 py-2 text-sm"
            />
          </div>
          <div>
            <p className="text-sm font-medium mb-1">새 비밀번호 확인</p>
            <input
              type="password"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              placeholder="새 비밀번호를 다시 입력해 주세요"
              className="w-full border rounded-lg px-3 py-2 text-sm"
            />
          </div>
          {error && <p className="text-xs text-red-500">{error}</p>}
          <div className="flex gap-2">
            <button
              onClick={handleBack}
              disabled={isSubmitting}
              className="px-4 py-2 rounded-lg border text-sm font-medium disabled:opacity-50"
            >
              다시 입력
            </button>
            <button
              onClick={handleReset}
              disabled={isSubmitting}
              className="px-4 py-2 rounded-lg bg-purple-600 text-white text-sm font-medium disabled:opacity-50"
            >
              {isSubmitting ? "변경 중..." : "비밀번호 변경"}
            </button>
          </div>
        </>
      )}
    </div>
  );
};

export const FindAccountForm = () => {
  const [mode, setMode] = useState<"id" | "password">("id");

  return (
    <div className="border rounded-xl p-6">
      <div className="flex gap-2 mb-4">
        <button
          onClick={() => setMode("id")}
          className={`flex-1 py-2 rounded-lg text-sm ${mode === "id" ? "bg-purple-600 text-white" : "border"}`}
        >
          아이디 찾기
        </button>
        <button
          onClick={() => setMode("password")}
          className={`flex-1 py-2 rounded-lg text-sm ${mode === "password" ? "bg-purple-600 text-white" : "border"}`}
        >
          비밀번호 찾기
        </button>
      </div>

      {mode === "id" && <IdFindPanel />}
      {mode === "password" && <PasswordFindPanel />}
    </div>
  );
};
