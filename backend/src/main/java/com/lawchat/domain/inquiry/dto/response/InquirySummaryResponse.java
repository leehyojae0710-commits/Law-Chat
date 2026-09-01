package com.lawchat.domain.inquiry.dto.response;

import com.lawchat.domain.inquiry.entity.Inquiry;
import com.lawchat.domain.inquiry.entity.InquiryCategory;
import com.lawchat.domain.inquiry.entity.InquiryStatus;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 문의함 목록용 요약 응답.
 *
 * NoticeListResponse 와 동일하게 categoryLabel 을 함께 내려 프론트가 매핑 테이블을 두지 않게 한다.
 * status 는 DB 컬럼이 아니라 계산값이며, statusLabel 도 같은 이유로 함께 내려준다.
 * 본문(content)과 답변(answerContent)은 목록에 담지 않는다 — 상세에서 조회한다.
 */
@Getter
public class InquirySummaryResponse {

    private final Long inquiryId;
    private final InquiryCategory category;
    private final String categoryLabel;
    private final String title;
    private final InquiryStatus status;
    private final String statusLabel;
    private final LocalDateTime createdAt;
    private final LocalDateTime answeredAt;

    private InquirySummaryResponse(Inquiry inquiry) {
        this.inquiryId = inquiry.getInquiryId();
        this.category = inquiry.getCategory();
        this.categoryLabel = inquiry.getCategory().getLabel();
        this.title = inquiry.getTitle();
        this.status = inquiry.getStatus();
        this.statusLabel = inquiry.getStatus().getLabel();
        this.createdAt = inquiry.getCreatedAt();
        // 답변이 아직 공개 전이면 답변 시각도 노출하지 않는다.
        this.answeredAt = (inquiry.getStatus() == InquiryStatus.ANSWERED) ? inquiry.getAnsweredAt() : null;
    }

    public static InquirySummaryResponse from(Inquiry inquiry) {
        return new InquirySummaryResponse(inquiry);
    }
}
