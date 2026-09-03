package com.lawchat.domain.inquiry.dto.response;

import com.lawchat.domain.inquiry.entity.Inquiry;
import com.lawchat.domain.inquiry.entity.InquiryCategory;
import com.lawchat.domain.inquiry.entity.InquiryStatus;
import com.lawchat.global.file.FileUrls;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 사용자용 문의 상세 응답.
 *
 * screenshotUrl 은 DB에 파일명만 저장되어 있으므로 FileUrls.view() 로 절대 URL 을 만들어 내려준다.
 * 스크린샷은 이미지라 브라우저에서 바로 렌더링해야 하므로 download() 가 아니라 view() 를 쓴다.
 *
 * 아직 공개되지 않은 답변(is_approved = 0)은 answerContent 를 null 로 감춘다.
 * 관리자가 작성 중인 답변 초안이 사용자에게 새어 나가지 않도록 하기 위함이다.
 */
@Getter
public class InquiryDetailResponse {

    private final Long inquiryId;
    private final InquiryCategory category;
    private final String categoryLabel;
    private final String title;
    private final String content;
    private final String screenshotUrl;
    private final InquiryStatus status;
    private final String statusLabel;
    private final String answerContent;
    private final LocalDateTime createdAt;
    private final LocalDateTime answeredAt;

    private InquiryDetailResponse(Inquiry inquiry) {
        boolean visible = inquiry.getStatus() == InquiryStatus.ANSWERED;

        this.inquiryId = inquiry.getInquiryId();
        this.category = inquiry.getCategory();
        this.categoryLabel = inquiry.getCategory().getLabel();
        this.title = inquiry.getTitle();
        this.content = inquiry.getContent();
        this.screenshotUrl = FileUrls.view(inquiry.getScreenshotUrl());
        this.status = inquiry.getStatus();
        this.statusLabel = inquiry.getStatus().getLabel();
        this.answerContent = visible ? inquiry.getAnswerContent() : null;
        this.answeredAt = visible ? inquiry.getAnsweredAt() : null;
        this.createdAt = inquiry.getCreatedAt();
    }

    public static InquiryDetailResponse from(Inquiry inquiry) {
        return new InquiryDetailResponse(inquiry);
    }
}
