import { apiClient } from "./client";

// ===== 대시보드 =====
export interface DashboardStats {
  totalDislikes: number;
  weeklyDislikes: number;
  totalLikes: number;
  dislikeRate: number;
}

export const getDashboardStats = async (): Promise<DashboardStats> => {
  const res = await apiClient.get<DashboardStats>("/admin/dashboard/stats");
  return res.data;
};

// ===== 1:1 문의 처리 =====
export interface Inquiry {
  id: string;
  title: string;
  content: string;
  authorEmail: string;
  status: "미답변" | "답변완료";
  createdAt: string;
}

export const getInquiries = async (): Promise<Inquiry[]> => {
  const res = await apiClient.get<Inquiry[]>("/admin/inquiries");
  return res.data;
};

export const answerInquiry = async (inquiryId: string, answer: string): Promise<void> => {
  await apiClient.post(`/admin/inquiries/${inquiryId}/answer`, { answer });
};

// ===== 공지사항 관리 =====
export interface Notice {
  id: string;
  title: string;
  content: string;
  category: string;
  isVisible: boolean;
  startDate?: string;
  endDate?: string;
}

export const getAdminNotices = async (): Promise<Notice[]> => {
  const res = await apiClient.get<Notice[]>("/admin/notices");
  return res.data;
};

export const createNotice = async (notice: Omit<Notice, "id">): Promise<void> => {
  await apiClient.post("/admin/notices", notice);
};

export const updateNotice = async (id: string, notice: Partial<Notice>): Promise<void> => {
  await apiClient.put(`/admin/notices/${id}`, notice);
};

export const deleteNotice = async (id: string): Promise<void> => {
  await apiClient.delete(`/admin/notices/${id}`);
};
