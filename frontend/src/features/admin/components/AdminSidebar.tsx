import type { AdminTab } from "../types";

interface AdminSidebarProps {
  active: AdminTab;
  onSelect: (tab: AdminTab) => void;
}

const tabs: { key: AdminTab; label: string }[] = [
  { key: "dashboard", label: "대시보드" },
  { key: "inquiries", label: "문의 처리하기" },
  { key: "content", label: "콘텐츠 관리" },
  { key: "notices", label: "공지사항 관리" },
];

export const AdminSidebar = ({ active, onSelect }: AdminSidebarProps) => {
  return (
    <nav className="space-y-1">
      {tabs.map((tab) => (
        <button
          key={tab.key}
          onClick={() => onSelect(tab.key)}
          className={`w-full text-left px-4 py-2.5 rounded-lg text-sm font-medium ${
            active === tab.key ? "bg-violet-600 text-white" : "text-gray-600 hover:bg-gray-100"
          }`}
        >
          {tab.label}
        </button>
      ))}
    </nav>
  );
};
