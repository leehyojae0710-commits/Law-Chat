package com.lawchat.domain.user.entity;

/**
 * 회원 상태 값.
 *
 * DB의 users.status 컬럼이 ENUM('ACTIVE','LOGOUT','DELETED') 이므로
 * 자바 enum 상수 이름과 DB 문자열이 1:1로 일치해야 한다.
 * 그래서 엔티티에서 @Enumerated(EnumType.STRING) 을 반드시 붙인다.
 * (기본값인 ORDINAL 로 두면 0,1,2 숫자로 저장돼서 DB ENUM 과 깨진다.)
 */
public enum UserStatus {

    /** 정상 이용 중인 회원 */
    ACTIVE,

    /** 로그아웃 상태 (설계상 세션 상태를 status 로 표현) */
    LOGOUT,

    /** 탈퇴한 회원. deleted_at 에 탈퇴 시각이 함께 기록된다. */
    DELETED
}
