import { notices } from "../data";

export const RecentNoticePanel = () => {
  return (
    <div className="border rounded-xl p-4 self-start">
      <p className="font-semibold mb-3">최근 공지</p>

      <div className="space-y-3">
        {notices.map((n) => (
          <div key={n.id} className="border-b pb-3">
            <p className="text-sm mb-1">{n.title}</p>
            <span className="text-xs text-gray-400">
              {n.date}
            </span>
          </div>
        ))}
      </div>
    </div>
  );
};