package com.lawchat.domain.admin.dto.response;

import java.util.List;

/**
 * GET /api/admin/dashboard/stats 응답.
 *
 * 좋아요는 집계하지 않는다. reason이 비어있는 행(👍 클릭분)은 완전히 무시하고,
 * 사유(reasonCode)가 있는 "싫어요" 신고만 집계 대상으로 삼는다.
 * -> 총 신고건수 / 이번 주 신고건수 / 사유 카테고리별 분포.
 */
public record AdminDashboardStatsResponse(
        long totalFeedbackCount,      // reason이 있는(=싫어요 신고) 전체 건수
        long weeklyFeedbackCount,     // 그 중 최근 7일 이내 건수
        List<ReasonStat> reasonBreakdown
) {
    public record ReasonStat(String code, String label, long count) {
    }
}
