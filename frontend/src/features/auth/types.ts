export interface LoginPayload {
  email: string;
  password: string;
}

export interface SignupPayload {
  email: string;
  password: string;
  passwordConfirm: string;
}

export interface AuthUser {
  id: string;
  name: string;
  email: string;
  role: "USER" | "ADMIN";
}
<<<<<<< HEAD
=======

export interface mockUser {
  id: string;
  name: string;
  password: string;
  email: string;
  role: "USER" | "ADMIN";
}
>>>>>>> 9f52ea2cf75bc8ac6461bd6cc3c9f94a0c772eff
