export interface LoginPayload {
  email: string;
  password: string;
}

export interface SignupPayload {
  email: string;
  nickname: string;
  password: string;
  passwordConfirm: string;
  // 백엔드 SignupRequest.phone (필수) - 아이디 찾기/비밀번호 재설정 인증 수단으로 쓰임.
  // 하이픈 포함/미포함 둘 다 허용, 서버에서 숫자만 남기도록 정규화한다.
  phone: string;
}

export interface AuthUser {
  id: string;
  name: string;
  email: string;
  isAdmin: boolean;
}

export interface mockUser {
  id: string;
  name: string;
  password: string;
  email: string;
  role: "USER" | "ADMIN";
}
