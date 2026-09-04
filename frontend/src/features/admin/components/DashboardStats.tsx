import { useDashboardStats } from "../hooks/useDashboardStats";
import type { DashboardRecentFeedbackItem } from "../../../api/admin";

// 사유 코드별 배지/막대 색상. reasonBreakdown·recentFeedback 둘 다 이 코드로 매핑해서 쓴다.
const REASON_STYLE: Record<
  string,
  { badgeLabel: string; badgeBg: string; badgeText: string; barColor: string }
> = {
  TERM_MISMATCH: { badgeLabel: "용어불일치", badgeBg: "bg-blue-50", badgeText: "text-blue-600", barColor: "bg-blue-500" },
  WRONG_CATEGORY: { badgeLabel: "분류오류", badgeBg: "bg-emerald-50", badgeText: "text-emerald-600", barColor: "bg-emerald-500" },
  WRONG_SOURCE: { badgeLabel: "근거오류", badgeBg: "bg-violet-50", badgeText: "text-violet-600", barColor: "bg-violet-500" },
  OFF_INTENT: { badgeLabel: "의도불일치", badgeBg: "bg-amber-50", badgeText: "text-amber-600", barColor: "bg-amber-500" },
  OTHER: { badgeLabel: "기타", badgeBg: "bg-gray-100", badgeText: "text-gray-500", barColor: "bg-gray-400" },
};

const formatUpdatedAt = (iso: string) => {
  const d = new Date(iso);
  const weekday = ["일", "월", "화", "수", "목", "금", "토"][d.getDay()];
  const date = d.toLocaleDateString("ko-KR", { year: "numeric", month: "2-digit", day: "2-digit" }).replace(/\. /g, ".").replace(/\.$/, "");
  const time = d.toLocaleTimeString("ko-KR", { hour: "2-digit", minute: "2-digit", hour12: false });
  return { dateLabel: `${date} (${weekday})`, timeLabel: time };
};

const formatFeedbackTime = (iso: string) => {
  const d = new Date(iso);
  const now = new Date();
  const isToday = d.toDateString() === now.toDateString();
  if (isToday) {
    return d.toLocaleTimeString("ko-KR", { hour: "2-digit", minute: "2-digit", hour12: false });
  }
  const yesterday = new Date(now);
  yesterday.setDate(now.getDate() - 1);
  if (d.toDateString() === yesterday.toDateString()) return "어제";
  return d.toLocaleDateString("ko-KR", { month: "numeric", day: "numeric" });
};

const FeedbackRow = ({ item }: { item: DashboardRecentFeedbackItem }) => {
  const style = REASON_STYLE[item.reasonCode] ?? REASON_STYLE.OTHER;
  return (
    <div className="flex items-center gap-3 py-3 border-t first:border-t-0">
      <span className={`shrink-0 w-20 text-center text-xs font-semibold px-2 py-1 rounded-md ${style.badgeBg} ${style.badgeText}`}>
        {style.badgeLabel}
      </span>
      <p className="shrink-0 w-48 text-sm font-medium text-gray-800 truncate">{item.title}</p>
      <p className="flex-1 text-xs text-gray-400 truncate">아쉬운 점: {item.reasonDetail}</p>
      <span className="shrink-0 text-xs text-gray-300 w-10">{formatFeedbackTime(item.createdAt)}</span>
      <button className="shrink-0 text-xs text-gray-500 border rounded-full px-3 py-1 hover:bg-gray-50">보기</button>
    </div>
  );
};

export const DashboardStats = () => {
  const { stats, isLoading } = useDashboardStats();

  if (isLoading) return <p className="text-sm text-gray-400">불러오는 중...</p>;
  if (!stats) return <p className="text-sm text-gray-400">데이터를 불러오지 못했습니다.</p>;

  const { dateLabel, timeLabel } = formatUpdatedAt(stats.updatedAt);

  return (
    <div className="space-y-5">
      {/* 헤더 + 갱신 시각 */}
      <div className="flex items-baseline justify-between">
        <h1 className="text-lg font-bold text-gray-900">대시보드</h1>
        <p className="text-xs text-gray-400">
          {dateLabel} · 최근 갱신 <span className="font-medium text-gray-500">{timeLabel}</span>
        </p>
      </div>

      {/* 상단 요약 카드 3개 */}
      <div className="grid grid-cols-3 gap-5">
        <div className="border rounded-xl p-5">
          <div className="flex items-center gap-2 mb-2">
            <span className="w-4 h-4 rounded-full bg-rose-200" />
            <p className="text-xs text-gray-500">총 싫어요 답변</p>
          </div>
          <p className="text-2xl font-bold text-rose-500">
            {stats.totalFeedbackCount.toLocaleString()} <span className="text-sm font-medium text-gray-400">건</span>
          </p>
          <p className="text-xs text-gray-400 mt-1">누적 전체 답변 중</p>
        </div>

        <div className="border rounded-xl p-5">
          <div className="flex items-center gap-2 mb-2">
            <span className="w-4 h-4 rounded-full bg-amber-200" />
            <p className="text-xs text-gray-500">이번 주 싫어요</p>
          </div>
          <p className="text-2xl font-bold text-amber-500">
            {stats.weeklyFeedbackCount.toLocaleString()} <span className="text-sm font-medium text-gray-400">건</span>
          </p>
          <p className="text-xs text-gray-400 mt-1">최근 7일 기준</p>
        </div>

        <div className="border rounded-xl p-5">
          <div className="flex items-center gap-2 mb-2">
            <span className="w-4 h-4 rounded-full bg-emerald-200" />
            <p className="text-xs text-gray-500">싫어요 비율</p>
          </div>
          <p className="text-2xl font-bold text-emerald-500">{stats.dislikeRatioPercent}%</p>
          <p className="text-xs text-gray-400 mt-1">전체 답변 대비</p>
        </div>
      </div>

      {/* 최근 피드백 + 분포 */}
      <div className="grid grid-cols-[1.6fr_1fr] gap-5 items-start">
        <div className="border rounded-xl p-5">
          <div className="flex items-center justify-between mb-2">
            <p className="text-sm font-bold text-gray-900">최근 싫어요 피드백</p>
            <button className="text-xs text-gray-500 border rounded-lg px-3 py-1.5 hover:bg-gray-50">전체 보기</button>
          </div>
          {stats.recentFeedback.length === 0 ? (
            <p className="text-sm text-gray-400 py-6 text-center">최근 신고된 피드백이 없습니다.</p>
          ) : (
            <div>
              {stats.recentFeedback.map((item) => (
                <FeedbackRow key={item.feedbackId} item={item} />
              ))}
            </div>
          )}
        </div>

        <div className="border rounded-xl p-5">
          <p className="text-sm font-bold text-gray-900 mb-3">아쉬운 점 분류별 분포</p>
          <div className="space-y-3">
            {stats.reasonBreakdown.map((reason) => {
              const style = REASON_STYLE[reason.code] ?? REASON_STYLE.OTHER;
              return (
                <div key={reason.code} className="flex items-center gap-3">
                  <p className="shrink-0 w-20 text-xs text-gray-500 truncate">{style.badgeLabel}</p>
                  <div className="flex-1 h-2 rounded-full bg-gray-100 overflow-hidden">
                    <div className={`h-full rounded-full ${style.barColor}`} style={{ width: `${reason.percent}%` }} />
                  </div>
                  <p className="shrink-0 w-10 text-right text-xs font-semibold text-gray-700">{reason.percent}%</p>
                </div>
              );
            })}
          </div>
        </div>
      </div>
    </div>
  );
};