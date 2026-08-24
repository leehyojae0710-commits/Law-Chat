export interface LoginPayload {
  email: string;
  password: string;
}

export interface SignupPayload {
  email: string;
  nickname:string;
  password: string;
  passwordConfirm: string;
}

export interface AuthUser {
  id: string;
  name: string;
  email: string;
  role: "USER" | "ADMIN";
}

export interface mockUser {
  id: string;
  name: string;
  password: string;
  email: string;
  role: "USER" | "ADMIN";
}

