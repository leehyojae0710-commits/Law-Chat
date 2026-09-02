package com.lawchat.domain.notice.dto.response;

import com.lawchat.domain.notice.entity.Notice;
import com.lawchat.domain.notice.entity.NoticeCategory;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 목록용 요약 응답.
 * categoryLabel 을 함께 내려 프론트가 enum -> 한글 매핑 테이블을 따로 두지 않도록 한다.
 */
@Getter
public class NoticeListResponse {

    private final Long noticeId;
    private final NoticeCategory category;
    private final String categoryLabel;
    private final String title;
    private final Boolean isPinned;
    private final LocalDateTime createdAt;

    private NoticeListResponse(Notice notice) {
        this.noticeId = notice.getNoticeId();
        this.category = notice.getCategory();
        this.categoryLabel = notice.getCategory().getLabel();
        this.title = notice.getTitle();
        this.isPinned = notice.getIsPinned();
        this.createdAt = notice.getCreatedAt();
    }

    public static NoticeListResponse from(Notice notice) {
        return new NoticeListResponse(notice);
    }
}
