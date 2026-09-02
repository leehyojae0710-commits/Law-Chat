package com.lawchat.domain.inquiry.entity;

import lombok.Getter;

/**
 * inquiries.category (varchar(50)) 에 enum 이름(BUG 등)이 문자열로 저장된다.
 *
 * label 은 화면에 그대로 찍을 한글 문구다. NoticeCategory 와 동일하게
 * 응답 DTO 에 categoryLabel 로 함께 내려주어 프론트가 매핑 테이블을 들고 있지 않도록 한다.
 */
@Getter
public enum InquiryCategory {

    BUG("버그 제보"),
    USAGE("이용 문의"),
    BILLING("결제·요금"),
    ACCOUNT("계정"),
    ETC("기타");

    private final String label;

    InquiryCategory(String label) {
        this.label = label;
    }
}
