package com.lawchat.domain.inquiry.domain;

/**
 * inquiries.category 에 문자열 그대로 저장됩니다. (varchar(50))
 * 프론트 features/support/types.ts 의 InquiryCategory 와 값이 일치해야 합니다.
 */
public enum InquiryCategory {
    BUG,      // 버그 제보
    USAGE,    // 이용 문의
    BILLING,  // 결제·요금
    ACCOUNT,  // 계정
    ETC       // 기타
}
