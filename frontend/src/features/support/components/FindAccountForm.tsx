import { useState } from "react";

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
      {/* TODO: 이름, 이메일, 인증코드 입력 폼 */}
    </div>
  );
};
