package com.lawchat.domain.inquiry.dto.response;

import com.lawchat.domain.inquiry.entity.Inquiry;
import com.lawchat.domain.inquiry.entity.InquiryCategory;
import com.lawchat.domain.inquiry.entity.InquiryStatus;
import com.lawchat.global.file.FileUrls;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 관리자용 문의 응답.
 *
 * 사용자용과 달리 미공개 답변(is_approved = 0)도 그대로 보여준다.
 * 관리자는 작성 중인 답변을 이어서 수정해야 하기 때문이다.
 *
 * authorEmail / authorNickname 은 null 일 수 있다.
 *  - 탈퇴 회원: inquiries.user_id 가 ON DELETE SET NULL 로 끊겨 user 자체가 null
 *  - 익명화된 회원: user 는 있으나 email 이 null (User.anonymize 참고)
 * 프론트는 두 경우 모두 "탈퇴한 회원" 등으로 표시하면 된다.
 */
@Getter
public class InquiryAdminResponse {

    private final Long inquiryId;
    private final InquiryCategory category;
    private final String categoryLabel;
    private final String title;
    private final String content;
    private final String screenshotUrl;
    private final Long authorId;
    private final String authorEmail;
    private final String authorNickname;
    private final InquiryStatus status;
    private final String statusLabel;
    private final String answerContent;
    private final LocalDateTime answeredAt;
    private final LocalDateTime createdAt;

    private InquiryAdminResponse(Inquiry inquiry) {
        this.inquiryId = inquiry.getInquiryId();
        this.category = inquiry.getCategory();
        this.categoryLabel = inquiry.getCategory().getLabel();
        this.title = inquiry.getTitle();
        this.content = inquiry.getContent();
        this.screenshotUrl = FileUrls.view(inquiry.getScreenshotUrl());
        this.authorId = (inquiry.getUser() != null) ? inquiry.getUser().getUserId() : null;
        this.authorEmail = (inquiry.getUser() != null) ? inquiry.getUser().getEmail() : null;
        this.authorNickname = (inquiry.getUser() != null) ? inquiry.getUser().getNickname() : null;
        this.status = inquiry.getStatus();
        this.statusLabel = inquiry.getStatus().getLabel();
        this.answerContent = inquiry.getAnswerContent();
        this.answeredAt = inquiry.getAnsweredAt();
        this.createdAt = inquiry.getCreatedAt();
    }

    public static InquiryAdminResponse from(Inquiry inquiry) {
        return new InquiryAdminResponse(inquiry);
    }
}
