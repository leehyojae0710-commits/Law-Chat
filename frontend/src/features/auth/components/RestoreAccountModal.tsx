import { useState } from "react";
import { useNavigate } from "react-router-dom";
import axios from "axios";
import { sendRestoreCode, verifyRestoreCode, type ContactType } from "../../../api/verification";
import { restoreAccount } from "../../../api/auth";

interface RestoreAccountModalProps {
  message: string;
  verifyBy: ContactType;
  verifyTarget: string;
  onClose: () => void;
}

type Step = "confirm" | "code" | "done";

const extractErrorMessage = (err: unknown, fallback: string): string => {
  if (axios.isAxiosError(err) && err.response?.data && typeof err.response.data === "object") {
    const data = err.response.data as { message?: string };
    if (data.message) return data.message;
  }
  return fallback;
};

export const RestoreAccountModal = ({ message, verifyBy, verifyTarget, onClose }: RestoreAccountModalProps) => {
  const navigate = useNavigate();
  const [step, setStep] = useState<Step>("confirm");
  const [code, setCode] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  const handleCodeChange = (v: string) => setCode(v.replace(/\D/g, "").slice(0, 6));
  const isCodeValid = () => /^\d{6}$/.test(code);

  // ① 인증코드 발송
  const handleStartRestore = async () => {
    setError(null);
    setIsSubmitting(true);
    try {
      const res = await sendRestoreCode(verifyBy, verifyTarget);
      setNotice(res.message);
      setStep("code");
    } catch (err) {
      setError(extractErrorMessage(err, "인증코드 발송에 실패했습니다. 다시 시도해 주세요."));
    } finally {
      setIsSubmitting(false);
    }
  };

  // ②③ 인증코드 확인 → 복구
  const handleVerifyAndRestore = async () => {
    if (!isCodeValid()) {
      setError("인증코드는 숫자 6자리입니다.");
      return;
    }
    setError(null);
    setIsSubmitting(true);
    try {
      await verifyRestoreCode(verifyBy, verifyTarget, code);
      await restoreAccount(verifyTarget);
      setStep("done");
    } catch (err) {
      setError(extractErrorMessage(err, "복구에 실패했습니다. 다시 시도해 주세요."));
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleGoLogin = () => {
    onClose();
    navigate("/login");
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 px-4">
      <div className="w-full max-w-[420px] rounded-xl bg-white p-6 shadow-lg space-y-4">
        {step === "confirm" && (
          <>
            <h3 className="text-lg font-bold">계정 복구</h3>
            <p className="text-sm text-gray-600 leading-relaxed">{message}</p>
            {error && <p className="text-sm text-red-500">{error}</p>}
            <div className="flex gap-2 pt-2">
              <button
                type="button"
                onClick={onClose}
                className="flex-1 py-2.5 rounded-lg border text-sm font-medium"
              >
                아니오
              </button>
              <button
                type="button"
                onClick={handleStartRestore}
                disabled={isSubmitting}
                className="flex-1 py-2.5 rounded-lg bg-violet-600 text-white text-sm font-medium disabled:opacity-50"
              >
                {isSubmitting ? "발송 중..." : "예, 복구할게요"}
              </button>
            </div>
          </>
        )}

        {step === "code" && (
          <>
            <h3 className="text-lg font-bold">인증코드 입력</h3>
            {notice && <p className="text-xs text-gray-500">{notice}</p>}
            <div>
              <p className="text-sm font-medium mb-1">인증코드</p>
              <input
                value={code}
                onChange={(e) => handleCodeChange(e.target.value)}
                placeholder="숫자 6자리"
                maxLength={6}
                inputMode="numeric"
                className="w-full border rounded-lg px-3 py-2 text-sm"
              />
            </div>
            {error && <p className="text-sm text-red-500">{error}</p>}
            <div className="flex gap-2 pt-2">
              <button
                type="button"
                onClick={onClose}
                className="flex-1 py-2.5 rounded-lg border text-sm font-medium"
              >
                취소
              </button>
              <button
                type="button"
                onClick={handleVerifyAndRestore}
                disabled={isSubmitting}
                className="flex-1 py-2.5 rounded-lg bg-violet-600 text-white text-sm font-medium disabled:opacity-50"
              >
                {isSubmitting ? "복구 중..." : "확인 및 복구"}
              </button>
            </div>
          </>
        )}

        {step === "done" && (
          <>
            <h3 className="text-lg font-bold text-center">계정이 복구됐어요</h3>
            <p className="text-sm text-gray-500 text-center">다시 로그인해 주세요.</p>
            <button
              type="button"
              onClick={handleGoLogin}
              className="w-full py-2.5 rounded-lg bg-violet-600 text-white text-sm font-medium mt-2"
            >
              로그인하러 가기
            </button>
          </>
        )}
      </div>
    </div>
  );
};