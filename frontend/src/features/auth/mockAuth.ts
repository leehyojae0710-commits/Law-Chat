// 백엔드가 완성되기 전, 프론트만 테스트하기 위한 mock 인증 로직입니다.
// 실제 API 연동이 끝나면 useAuth.ts에서 VITE_USE_MOCK_AUTH=false 로만 바꾸면 되고,
// 이 파일은 더 이상 호출되지 않습니다 (지워도 무방).
import type { AuthResponse } from "../../api/auth";
import type { LoginPayload, SignupPayload } from "./types";
import { mockUsers } from "./data";

// 회원가입 테스트 시 추가되는 계정을 담아두는 메모리 저장소입니다.
// (새로고침하면 초기화됩니다 — 영구 저장이 필요하면 data.ts에 직접 추가하세요.)
const runtimeUsers = [...mockUsers];

// 실제 네트워크 호출처럼 약간의 지연을 흉내내서, 로딩 UI도 함께 테스트할 수 있게 합니다.
const fakeDelay = (ms = 400) => new Promise((resolve) => setTimeout(resolve, ms));

export const mockLogin = async (payload: LoginPayload): Promise<AuthResponse> => {
  await fakeDelay();

  const found = runtimeUsers.find(
    (u) => u.email === payload.email && u.password === payload.password
  );
  if (!found) {
    throw new Error("이메일 또는 비밀번호가 틀렸습니다");
  }

  const user = { id: found.id, name: found.name, email: found.email, role: found.role };
  const accessToken = "mock-token-" + Date.now();
  return { user, accessToken };
};

export const mockSignup = async (payload: SignupPayload): Promise<AuthResponse> => {
  await fakeDelay();

  if (payload.password !== payload.passwordConfirm) {
    throw new Error("비밀번호가 일치하지 않습니다");
  }
  if (runtimeUsers.some((u) => u.email === payload.email)) {
    throw new Error("이미 가입된 이메일입니다");
  }

  const newUser = {
    id: "mock-" + Date.now(),
    name: payload.email.split("@")[0],
    email: payload.email,
    password: payload.password,
    role: "USER" as const,
  };
  runtimeUsers.push(newUser);

  const user = { id: newUser.id, name: newUser.name, email: newUser.email, role: newUser.role };
  const accessToken = "mock-token-" + Date.now();
  return { user, accessToken };
};

export const mockLogout = async (): Promise<void> => {
  await fakeDelay(150);
};
