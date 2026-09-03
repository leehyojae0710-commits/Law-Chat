import type { SupportTab } from "../types";

interface SupportSideMenuProps {
  active: SupportTab;
  onSelect: (tab: SupportTab) => void;
}

const menuItems: { key: SupportTab; label: string }[] = [
  { key: "inquiry-form", label: "1:1 문의하기" },
  { key: "inquiry-list", label: "문의함" },
  { key: "find-account", label: "아이디·비밀번호 찾기" },
];

export const SupportSideMenu = ({ active, onSelect }: SupportSideMenuProps) => {
  return (
    <div className="space-y-4 border rounded-xl p-4">
      {menuItems.map((item) => (
        <button
          key={item.key}
          onClick={() => onSelect(item.key)}
          className={`flex items-center gap-2 text-sm ${active === item.key ? "text-purple-600 font-semibold" : "text-gray-500"
            }`}
        >
          <span className="w-2 h-2 rounded-full bg-current" />
          {item.label}
        </button>
      ))}
    </div>
  );
};
