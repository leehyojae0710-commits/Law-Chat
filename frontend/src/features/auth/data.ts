import type { mockUser } from "./types";

export const mockUsers: mockUser[] = [
  { id: "admin", name: "Admin", password: "1234", email: "admin@test.com", role: "ADMIN" },
  { id: "user1", name: "User1", password: "1234", email: "user1@test.com", role: "USER" },
];