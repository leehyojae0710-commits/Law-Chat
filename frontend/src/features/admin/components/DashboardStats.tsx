import { useDashboardStats } from "../hooks/useDashboardStats";

export const DashboardStats = () => {
  const { stats, isLoading } = useDashboardStats();

  if (isLoading) return <p className="text-sm text-gray-400">불러오는 중...</p>;
  if (!stats) return <p className="text-sm text-gray-400">데이터를 불러오지 못했습니다.</p>;

  const summaryItems = [
    { label: "총 신고(싫어요) 건수", value: stats.totalFeedbackCount.toLocaleString() },
    { label: "이번 주 신고", value: `${stats.weeklyFeedbackCount}건` },
  ];

  return (
    <div className="space-y-6">
      <div className="grid grid-cols-2 gap-4">
        {summaryItems.map((item) => (
          <div key={item.label} className="border rounded-xl p-5">
            <p className="text-xs text-gray-400 mb-2">{item.label}</p>
            <p className="text-2xl font-bold">{item.value}</p>
          </div>
        ))}
      </div>

      <div>
        <p className="mb-3 text-sm font-medium text-slate-700">사유별 분포</p>
        <div className="grid grid-cols-5 gap-4">
          {stats.reasonBreakdown.map((reason) => (
            <div key={reason.code} className="border rounded-xl p-4">
              <p className="text-xs text-gray-400 mb-2">{reason.label}</p>
              <p className="text-xl font-bold">{reason.count.toLocaleString()}건</p>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};