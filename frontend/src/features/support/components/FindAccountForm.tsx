import { useState } from "react";
import axios from "axios";
import {
  sendPasswordResetCode,
  resetPassword,
  sendIdFindCode,
  verifyIdFindCode,
  type ContactType,
} from "../../../api/verification";
import { useNavigate, Link } from "react-router-dom";

const PASSWORD_PATTERN = /^(?=.*[A-Za-z])(?=.*\d).{8,64}$/;

const extractErrorMessage = (err: unknown, fallback: string): string => {
  if (axios.isAxiosError(err) && err.response?.data && typeof err.response.data === "object") {
    const data = err.response.data as { message?: string };
    if (data.message) return data.message;
  }
  return fallback;
};

const contactLabel = (type: ContactType) => (type === "EMAIL" ? "가입한 이메일" : "가입한 휴대폰 번호");
const contactPlaceholder = (type: ContactType) =>
  type === "EMAIL" ? "example@email.com" : "01012345678 (숫자만)";

// ============================================================
// 공통 훅 — 연락처/인증코드 상태 + 검증 로직
// ============================================================

function useContactVerification(initialType: ContactType = "EMAIL") {
  const [contactType, setContactType] = useState<ContactType>(initialType);
  const [contactValue, setContactValue] = useState("");
  const [code, setCode] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  const handleContactTypeChange = (v: ContactType) => {
    setContactType(v);
    setContactValue("");
    setError(null);
  };

  const handleCodeChange = (v: string) => {
    setCode(v.replace(/\D/g, "").slice(0, 6));
  };

  const isCodeValid = () => /^\d{6}$/.test(code);

  // 인증코드 발송 공통 처리 — 실제 API 호출 함수만 인자로 받는다
  const sendCode = async (
    sendFn: (type: ContactType, value: string) => Promise<{ message: string }>,
  ): Promise<boolean> => {
    const trimmed = contactValue.trim();
    if (!trimmed) {
      setError(contactType === "EMAIL" ? "이메일을 입력해 주세요." : "휴대폰 번호를 입력해 주세요.");
      return false;
    }
    setError(null);
    setIsSubmitting(true);
    try {
      const result = await sendFn(contactType, trimmed);
      setNotice(result.message);
      return true;
    } catch (err) {
      console.error("인증코드 발송 실패:", err);
      setError(extractErrorMessage(err, "인증코드 발송에 실패했습니다. 다시 시도해 주세요."));
      return false;
    } finally {
      setIsSubmitting(false);
    }
  };

  const resetContact = () => {
    setContactValue("");
    setCode("");
    setError(null);
    setNotice(null);
  };

  return {
    contactType,
    contactValue,
    code,
    isSubmitting,
    error,
    notice,
    setContactValue,
    setError,
    setNotice,
    setIsSubmitting,
    handleContactTypeChange,
    handleCodeChange,
    isCodeValid,
    sendCode,
    resetContact,
  };
}

type ContactVerification = ReturnType<typeof useContactVerification>;

// ============================================================
// 공통 UI 조각
// ============================================================

const ContactTypeToggle = ({
  value,
  onChange,
  disableEmail = false,
}: {
  value: ContactType;
  onChange: (v: ContactType) => void;
  disableEmail?: boolean;
}) => (
  <div className="flex gap-2 text-sm">
    <button
      type="button"
      onClick={() => onChange("EMAIL")}
      disabled={disableEmail}
      className={`px-3 py-1.5 rounded-full border ${
        value === "EMAIL" ? "bg-purple-600 text-white border-purple-600" : "text-gray-600"
      } ${disableEmail ? "opacity-40 cursor-not-allowed" : ""}`}
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

// "연락처 입력 → 인증코드 받기" 단계 — 두 패널에서 동일하게 사용
const ContactInputStep = ({
  v,
  onSubmit,
  onCancel,
  disableEmail = false,
}: {
  v: ContactVerification;
  onSubmit: () => void;
  onCancel: () => void;
  disableEmail?: boolean;
}) => (
  <>
    <ContactTypeToggle value={v.contactType} onChange={v.handleContactTypeChange} disableEmail={disableEmail} />
    <div>
      <p className="text-sm font-medium mb-1">{contactLabel(v.contactType)}</p>
      <input
        value={v.contactValue}
        onChange={(e) => v.setContactValue(e.target.value)}
        placeholder={contactPlaceholder(v.contactType)}
        inputMode={v.contactType === "PHONE" ? "numeric" : "email"}
        className="w-full border rounded-lg px-3 py-2 text-sm"
      />
    </div>
    {v.error && <p className="text-xs text-red-500">{v.error}</p>}
    <div className="space-x-3">
      <button
        onClick={onSubmit}
        disabled={v.isSubmitting}
        className="px-4 py-2 rounded-lg bg-purple-600 text-white text-sm font-medium disabled:opacity-50"
      >
        {v.isSubmitting ? "발송 중..." : "인증코드 받기"}
      </button>
      <button
        onClick={onCancel}
        className="px-4 py-2 rounded-lg bg-purple-600 text-white text-sm font-medium disabled:opacity-50"
      >
        돌아가기
      </button>
    </div>
  </>
);

// "인증코드 입력" 필드만 — 하단 버튼은 패널마다 다르므로 여기 포함 안 함
const CodeInputField = ({ v }: { v: ContactVerification }) => (
  <>
    {v.notice && <p className="text-xs text-gray-500">{v.notice}</p>}
    <div>
      <p className="text-sm font-medium mb-1">인증코드</p>
      <input
        value={v.code}
        onChange={(e) => v.handleCodeChange(e.target.value)}
        placeholder="숫자 6자리"
        maxLength={6}
        inputMode="numeric"
        className="w-full border rounded-lg px-3 py-2 text-sm"
      />
    </div>
    {v.error && <p className="text-xs text-red-500">{v.error}</p>}
  </>
);

// ============================================================
// 아이디(이메일) 찾기
// ============================================================

type IdFindStep = "input" | "verify" | "done";

const IdFindPanel = () => {
  const v = useContactVerification("PHONE");
  const [step, setStep] = useState<IdFindStep>("input");
  const [foundEmail, setFoundEmail] = useState("");
  const navigate = useNavigate();

  const handleSendCode = async () => {
    const ok = await v.sendCode(sendIdFindCode);
    if (ok) setStep("verify");
  };

  const handleVerify = async () => {
    if (!v.isCodeValid()) {
      v.setError("인증코드는 숫자 6자리입니다.");
      return;
    }
    v.setError(null);
    v.setIsSubmitting(true);
    try {
      const result = await verifyIdFindCode(v.contactType, v.contactValue.trim(), v.code);
      setFoundEmail(result.email);
      setStep("done");
      v.setNotice(null);
    } catch (err) {
      console.error("아이디 찾기 인증 실패:", err);
      v.setError(extractErrorMessage(err, "인증에 실패했습니다. 다시 시도해 주세요."));
    } finally {
      v.setIsSubmitting(false);
    }
  };

  const handleBack = () => {
    setStep("input");
    v.resetContact();
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
      {step === "input" && (
        <ContactInputStep v={v} onSubmit={handleSendCode} onCancel={() => navigate("/login")} disableEmail />
      )}
      {step === "verify" && (
        <>
          <CodeInputField v={v} />
          <div className="flex gap-2">
            <button
              onClick={handleBack}
              disabled={v.isSubmitting}
              className="px-4 py-2 rounded-lg border text-sm font-medium disabled:opacity-50"
            >
              다시 입력
            </button>
            <button
              onClick={handleVerify}
              disabled={v.isSubmitting}
              className="px-4 py-2 rounded-lg bg-purple-600 text-white text-sm font-medium disabled:opacity-50"
            >
              {v.isSubmitting ? "확인 중..." : "확인"}
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
  const v = useContactVerification();
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [step, setStep] = useState<PasswordStep>("input");
  const navigate = useNavigate();

  const handleSendCode = async () => {
    // 가입 여부와 무관하게 항상 success: true가 내려온다 (계정 열거 공격 방지).
    const ok = await v.sendCode(sendPasswordResetCode);
    if (ok) setStep("reset");
  };

  const handleResetPassword = async () => {
    if (!v.isCodeValid()) {
      v.setError("인증코드는 숫자 6자리입니다.");
      return;
    }
    if (!PASSWORD_PATTERN.test(newPassword)) {
      v.setError("비밀번호는 영문과 숫자를 모두 포함해 8~64자로 입력해 주세요.");
      return;
    }
    if (newPassword !== confirmPassword) {
      v.setError("새 비밀번호가 일치하지 않습니다.");
      return;
    }

    v.setError(null);
    v.setIsSubmitting(true);
    try {
      await resetPassword({
        contactType: v.contactType,
        contactValue: v.contactValue.trim(),
        code: v.code,
        newPassword,
      });
      setStep("done");
      v.setNotice(null);
    } catch (err) {
      console.error("비밀번호 재설정 실패:", err);
      v.setError(extractErrorMessage(err, "비밀번호 재설정에 실패했습니다. 다시 시도해 주세요."));
    } finally {
      v.setIsSubmitting(false);
    }
  };

  const handleBack = () => {
    setStep("input");
    setNewPassword("");
    setConfirmPassword("");
    v.resetContact();
  };

  if (step === "done") {
    return (
      <div className="space-y-3 text-center py-6">
        <p className="font-semibold">비밀번호가 변경되었습니다</p>
        <p className="text-sm text-gray-500">새 비밀번호로 로그인해 주세요.</p>
        <button
          onClick={() => navigate("/login")}
          className="px-4 py-2 rounded-lg bg-purple-600 text-white text-sm font-medium disabled:opacity-50"
        >
          돌아가기
        </button>
      </div>
    );
  }

  return (
    <div className="space-y-3">
      {step === "input" && (
        <ContactInputStep v={v} onSubmit={handleSendCode} onCancel={() => navigate("/login")} />
      )}
      {step === "reset" && (
        <>
          <CodeInputField v={v} />
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
          <div className="flex gap-2">
            <button
              onClick={handleBack}
              disabled={v.isSubmitting}
              className="px-4 py-2 rounded-lg border text-sm font-medium disabled:opacity-50"
            >
              다시 입력
            </button>
            <button
              onClick={handleResetPassword}
              disabled={v.isSubmitting}
              className="px-4 py-2 rounded-lg bg-purple-600 text-white text-sm font-medium disabled:opacity-50"
            >
              {v.isSubmitting ? "변경 중..." : "비밀번호 변경"}
            </button>
          </div>
        </>
      )}
    </div>
  );
};

// ============================================================
// 최상위 폼
// ============================================================

export const FindAccountForm = () => {
  const [mode, setMode] = useState<"id" | "password">("id");

  return (
    <div className="min-h-screen bg-violet-50 py-10">
      <div className="mx-auto max-w-[700px] my-auto min-h-[500px] border rounded-xl p-6 bg-white">
        <Link to="/" className="flex flex-col items-center gap-2 text-center">
          <div className="w-20 h-20 rounded-full bg-violet-600 text-white flex items-center justify-center font-bold text-4xl">
            L
          </div>
          <div>
            <p className="font-bold text-lg text-slate-900">LawChat</p>
            <p className="text-xs text-slate-400">AI 법률 상담 챗봇</p>
          </div>
        </Link>
        <div className="flex flex-col gap-2 mb-2 py-6">
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
    </div>
  );
};