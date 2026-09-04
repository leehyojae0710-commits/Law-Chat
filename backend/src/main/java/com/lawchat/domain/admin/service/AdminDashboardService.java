package com.lawchat.domain.admin.service;

import com.lawchat.domain.admin.dto.response.AdminDashboardStatsResponse;
import com.lawchat.domain.chat.entity.ChatFeedbackDataset;
import com.lawchat.domain.chat.entity.ChatRole;
import com.lawchat.domain.chat.repository.ChatFeedbackRepository;
import com.lawchat.domain.chat.repository.ChatMessageRepository;
import com.lawchat.global.auth.AdminValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 관리자 대시보드 통계.
 * chat_feedback_dataset 스키마: message_id, prompt, response, sources, reason, created_at
 * (is_positive 컬럼 없음 -> 좋아요/싫어요 비율이 아니라 신고 건수·사유별 분포로 집계)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDashboardService {

    private static final int RECENT_FEEDBACK_LIMIT = 6;
    private static final int TITLE_MAX_LENGTH = 24;

    private final ChatFeedbackRepository chatFeedbackRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final AdminValidator adminValidator;

    // FeedbackReasonCode (frontend/src/features/chat/types.ts)와 1:1로 맞춰둔 라벨.
    // 순서를 유지해 항상 같은 순서로 응답하도록 LinkedHashMap 사용.
    private static final Map<String, String> REASON_LABELS = new LinkedHashMap<>();

    static {
        REASON_LABELS.put("TERM_MISMATCH", "법률 용어 변환이 정확하지 않음");
        REASON_LABELS.put("WRONG_CATEGORY", "법률 분야가 잘못 분류됨");
        REASON_LABELS.put("WRONG_SOURCE", "근거 조문이나 판례가 사실과 다름");
        REASON_LABELS.put("OFF_INTENT", "질문 의도와 다른 답변");
        REASON_LABELS.put("OTHER", "기타");
    }

    public AdminDashboardStatsResponse getStats(Long userId) {
        adminValidator.validate(userId);

        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        List<ChatFeedbackDataset> all = chatFeedbackRepository.findAll();

        Map<String, Long> counts = new LinkedHashMap<>();
        REASON_LABELS.keySet().forEach(code -> counts.put(code, 0L));

        long totalFeedback = 0;
        long weeklyFeedback = 0;

        for (ChatFeedbackDataset f : all) {
            String code = extractReasonCode(f.getReason());
            if (code == null) {
                // reason이 비어있음 = 👍 좋아요 클릭. 좋아요는 집계하지 않으므로 완전히 건너뛴다.
                continue;
            }
            totalFeedback++;
            counts.merge(code, 1L, Long::sum);
            if (f.getCreatedAt() != null && !f.getCreatedAt().isBefore(weekAgo)) {
                weeklyFeedback++;
            }
        }

        long finalTotalFeedback = totalFeedback;
        List<AdminDashboardStatsResponse.ReasonStat> breakdown = REASON_LABELS.entrySet().stream()
                .map(e -> {
                    long count = counts.get(e.getKey());
                    double percent = finalTotalFeedback == 0 ? 0.0 : round1(count * 100.0 / finalTotalFeedback);
                    return new AdminDashboardStatsResponse.ReasonStat(e.getKey(), e.getValue(), count, percent);
                })
                .toList();

        long totalAiAnswers = chatMessageRepository.countByRole(ChatRole.AI);
        double dislikeRatioPercent = totalAiAnswers == 0 ? 0.0 : round1(totalFeedback * 100.0 / totalAiAnswers);

        List<AdminDashboardStatsResponse.RecentFeedbackItem> recentFeedback = chatFeedbackRepository
                .findRecentDislikes(PageRequest.of(0, RECENT_FEEDBACK_LIMIT))
                .stream()
                .map(this::toRecentFeedbackItem)
                .toList();

        return new AdminDashboardStatsResponse(
                totalFeedback,
                weeklyFeedback,
                dislikeRatioPercent,
                breakdown,
                recentFeedback,
                LocalDateTime.now()
        );
    }

    private AdminDashboardStatsResponse.RecentFeedbackItem toRecentFeedbackItem(ChatFeedbackDataset feedback) {
        String code = extractReasonCode(feedback.getReason());
        String normalizedCode = code == null ? "OTHER" : code;
        String label = REASON_LABELS.getOrDefault(normalizedCode, REASON_LABELS.get("OTHER"));
        String detail = extractReasonDetail(feedback.getReason());

        return new AdminDashboardStatsResponse.RecentFeedbackItem(
                feedback.getFeedbackId(),
                truncateTitle(feedback.getPrompt()),
                normalizedCode,
                label,
                detail != null ? detail : label,
                feedback.getCreatedAt()
        );
    }

    /**
     * reason 컬럼에서 상세설명만 뽑아낸다 (코드 접두어 제외).
     * - "TERM_MISMATCH: 상세설명" -> "상세설명"
     * - "[TERM_MISMATCH] 상세설명" -> "상세설명"
     * - 접두어뿐이거나 상세설명이 없으면 null.
     */
    private String extractReasonDetail(String reason) {
        if (reason == null || reason.isBlank()) {
            return null;
        }
        String trimmed = reason.trim();
        String detail;
        if (trimmed.startsWith("[")) {
            int end = trimmed.indexOf(']');
            detail = end >= 0 && end + 1 < trimmed.length() ? trimmed.substring(end + 1).trim() : "";
        } else if (trimmed.contains(":")) {
            detail = trimmed.substring(trimmed.indexOf(':') + 1).trim();
        } else {
            detail = "";
        }
        return detail.isBlank() ? null : detail;
    }

    /**
     * prompt(사용자 질문 원문)를 목록 카드용 짧은 제목으로 자른다.
     * 실제 "제목" 컬럼이 없어서 대체하는 것이므로, 줄바꿈은 공백으로 치환하고 길이만 제한한다.
     */
    private String truncateTitle(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            return "(제목 없음)";
        }
        String singleLine = prompt.trim().replaceAll("\\s+", " ");
        if (singleLine.length() <= TITLE_MAX_LENGTH) {
            return singleLine;
        }
        return singleLine.substring(0, TITLE_MAX_LENGTH) + "…";
    }

    private double round1(double value) {
        return Math.round(value * 10) / 10.0;
    }

    /**
     * reason 컬럼 값에서 사유 코드를 뽑아낸다.
     * - 프론트 실제 전송 형식(api/chat.ts): "TERM_MISMATCH" 또는 "TERM_MISMATCH: 상세설명"
     * - 더미 시드 데이터 형식(003_seed_chat_feedback_dummy_no_is_positive.sql): "[TERM_MISMATCH] 상세설명"
     * 두 형식을 모두 지원하고, 알 수 없는 값이거나 비어있으면 각각 OTHER / null 로 처리한다.
     *
     * @return 사유 코드. reason이 비어있으면(=좋아요로 추정) null.
     */
    private String extractReasonCode(String reason) {
        if (reason == null || reason.isBlank()) {
            return null;
        }
        String cleaned = reason.trim();
        if (cleaned.startsWith("[")) {
            int end = cleaned.indexOf(']');
            if (end > 1) {
                cleaned = cleaned.substring(1, end);
            }
        } else if (cleaned.contains(":")) {
            cleaned = cleaned.substring(0, cleaned.indexOf(':'));
        }
        cleaned = cleaned.trim();
        return REASON_LABELS.containsKey(cleaned) ? cleaned : "OTHER";
    }
}
