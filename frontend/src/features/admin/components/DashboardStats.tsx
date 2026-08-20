import { useDashboardStats } from "../hooks/useDashboardStats";

export const DashboardStats = () => {
  const { stats, isLoading } = useDashboardStats();

  if (isLoading) return <p className="text-sm text-gray-400">불러오는 중...</p>;
  if (!stats) return <p className="text-sm text-gray-400">데이터를 불러오지 못했습니다.</p>;

  const items = [
    { label: "총 싫어요 답변", value: stats.totalDislikes.toLocaleString() },
    { label: "이번 주 싫어요", value: `${stats.weeklyDislikes}건` },
    { label: "총 좋아요 답변", value: stats.totalLikes.toLocaleString() },
    { label: "싫어요 비율", value: `${stats.dislikeRate}%` },
  ];

  return (
    <div className="grid grid-cols-4 gap-4">
      {items.map((item) => (
        <div key={item.label} className="border rounded-xl p-5">
          <p className="text-xs text-gray-400 mb-2">{item.label}</p>
          <p className="text-2xl font-bold">{item.value}</p>
        </div>
      ))}
    </div>
  );
};
