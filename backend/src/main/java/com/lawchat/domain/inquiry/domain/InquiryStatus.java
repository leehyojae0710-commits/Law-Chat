package com.lawchat.inquiry.domain;

/**
 * DB에 status 컬럼이 없으므로 answered_at + is_approved 로 계산해서 내려주는 파생 값입니다.
 */
public enum InquiryStatus {
    PENDING,  // 답변대기
    ANSWERED  // 답변완료
}
