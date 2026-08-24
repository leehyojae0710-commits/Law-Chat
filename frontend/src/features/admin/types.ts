export interface AdminStat {
  id: string;
  label: string;
  value: string;
  helper?: string;
}

export type AdminTab = "dashboard" | "inquiries" | "content" | "notices";
