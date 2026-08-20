import { useState } from "react";
import type { AdminTab } from "../features/admin/types";
import { AdminSidebar } from "../features/admin/components/AdminSidebar";
import { DashboardStats } from "../features/admin/components/DashboardStats";
import { InquiryQueue } from "../features/admin/components/InquiryQueue";
import { NoticeEditor } from "../features/admin/components/NoticeEditor";
import { useAuthStore } from "../store/authStore";

export const AdminPage = () => {
  const [tab, setTab] = useState<AdminTab>("dashboard");
  const user = useAuthStore((s) => s.user);

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="max-w-6xl mx-auto px-8 py-8 grid grid-cols-[220px_1fr] gap-8">
        <div>
          <div className="mb-6">
            <p className="font-bold">LawChat Admin</p>
            <p className="text-xs text-gray-400">{user?.email}</p>
          </div>
          <AdminSidebar active={tab} onSelect={setTab} />
        </div>

        <div>
          {tab === "dashboard" && <DashboardStats />}
          {tab === "inquiries" && <InquiryQueue />}
          {tab === "notices" && <NoticeEditor />}
        </div>
      </div>
    </div>
  );
};
