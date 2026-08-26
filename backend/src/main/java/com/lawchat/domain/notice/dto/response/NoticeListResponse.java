package com.lawchat.domain.notice.dto.response;

import com.lawchat.domain.notice.entity.Notice;
import com.lawchat.domain.notice.entity.NoticeCategory;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class NoticeListResponse {

    private final Long noticeId;
    private final NoticeCategory category;
    private final String title;
    private final Boolean isPinned;
    private final LocalDateTime createdAt;

    private NoticeListResponse(Notice notice) {
        this.noticeId = notice.getNoticeId();
        this.category = notice.getCategory();
        this.title = notice.getTitle();
        this.isPinned = notice.getIsPinned();
        this.createdAt = notice.getCreatedAt();
    }

    public static NoticeListResponse from(Notice notice) {
        return new NoticeListResponse(notice);
    }
}
