import { useState } from "react";
import type { AdminTab } from "../features/admin/types";
import { AdminSidebar } from "../features/admin/components/AdminSidebar";
import { DashboardStats } from "../features/admin/components/DashboardStats";
import { InquiryQueue } from "../features/admin/components/InquiryQueue";
import { NoticeEditor } from "../features/admin/components/NoticeEditor";
import { useNavigate } from "react-router-dom";

export const AdminPage = () => {
  const [tab, setTab] = useState<AdminTab>("dashboard");
  const navigate = useNavigate();

  return (
    <div className="min-h-screen bg-violet-100">
      <div className="max-w-6xl mx-auto px-8 py-6 grid grid-cols-[220px_1fr] gap-8">
        {/* 사이드바 틀 */}
        <div className="bg-white border rounded-xl p-4 h-fit">
          <div
            onClick={() => navigate("/")}
            className="mb-6 cursor-pointer bg-violet-200 rounded-lg px-2 py-2"
          >
            <p className="font-bold">LawChat Admin</p>
          </div>
          <AdminSidebar active={tab} onSelect={setTab} />
        </div>

        {/* 컨텐츠 틀 */}
        <div className="bg-white border rounded-xl p-5">
          {tab === "dashboard" && <DashboardStats />}
          {tab === "inquiries" && <InquiryQueue />}
          {tab === "notices" && <NoticeEditor />}
        </div>
      </div>
    </div>
  );
};