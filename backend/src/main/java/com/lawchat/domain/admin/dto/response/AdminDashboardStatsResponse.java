package com.lawchat.domain.admin.dto.response;

import java.time.LocalDateTime;
import java.util.List;

/**
 * GET /api/admin/dashboard/stats 응답.
 *
 * 좋아요는 집계하지 않는다. reason이 비어있는 행(👍 클릭분)은 완전히 무시하고,
 * 사유(reasonCode)가 있는 "싫어요" 신고만 집계 대상으로 삼는다.
 * -> 총 신고건수 / 이번 주 신고건수 / 싫어요 비율 / 사유 카테고리별 분포 / 최근 피드백 목록.
 */
public record AdminDashboardStatsResponse(
        long totalFeedbackCount,      // reason이 있는(=싫어요 신고) 전체 건수
        long weeklyFeedbackCount,     // 그 중 최근 7일 이내 건수
        double dislikeRatioPercent,   // totalFeedbackCount / 전체 AI 답변 수 * 100 (전체 답변이 0이면 0)
        List<ReasonStat> reasonBreakdown,
        List<RecentFeedbackItem> recentFeedback, // 최신순 상위 N개
        LocalDateTime updatedAt       // 이 응답을 만든 시각 (프론트 "최근 갱신" 표시용)
) {
    public record ReasonStat(String code, String label, long count, double percent) {
    }

    public record RecentFeedbackItem(
            Long feedbackId,
            String title,        // prompt를 짧게 잘라낸 것 (실제 질문 제목 컬럼이 없어서 대체)
            String reasonCode,
            String reasonLabel,
            String reasonDetail, // "코드: 상세설명" 형식에서 상세설명만 뽑은 것. 없으면 reasonLabel과 동일
            LocalDateTime createdAt
    ) {
    }
}
