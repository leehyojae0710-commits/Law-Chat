package com.lawchat.domain.notice.entity;

import lombok.Getter;

/**
 * notices.category (varchar(20)) 에 enum 이름(SERVICE_UPDATE 등)이 문자열로 저장된다.
 *
 * label 은 화면에 그대로 찍을 한글 문구다. 프론트가 별도 매핑 테이블을 들고 있지 않도록
 * 응답 DTO 에 categoryLabel 로 함께 내려준다.
 * (프론트 화면설계서의 카테고리 탭 문구와 1:1로 맞춤)
 */
@Getter
public enum NoticeCategory {

    SERVICE_UPDATE("서비스 업데이트"),
    MAINTENANCE("점검 안내"),
    TERMS("이용약관");

    private final String label;

    NoticeCategory(String label) {
        this.label = label;
    }
}
