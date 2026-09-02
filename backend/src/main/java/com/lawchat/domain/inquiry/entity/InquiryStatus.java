package com.lawchat.domain.inquiry.entity;

import lombok.Getter;

/**
 * inquiries 테이블에는 status 컬럼이 없다.
 * answered_at 과 is_approved 로 계산해서 내려주는 파생 값이다. (Inquiry#getStatus)
 *
 * DB 컬럼이 아니므로 이 enum 은 절대 @Enumerated 로 매핑하지 않는다.
 */
@Getter
public enum InquiryStatus {

    PENDING("답변대기"),
    ANSWERED("답변완료");

    private final String label;

    InquiryStatus(String label) {
        this.label = label;
    }
}
