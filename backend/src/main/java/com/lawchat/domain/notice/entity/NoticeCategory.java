package com.lawchat.domain.notice.entity;

/**
 * notices.category (varchar(20)) 에 문자열로 매핑되는 enum.
 * 실제 운영에서 쓰는 카테고리에 맞춰 값만 조정하면 됨.
 */
public enum NoticeCategory {
    GENERAL,
    SYSTEM,
    EVENT
}
