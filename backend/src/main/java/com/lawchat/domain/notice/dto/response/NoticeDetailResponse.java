package com.lawchat.domain.notice.dto.response;

import com.lawchat.domain.notice.entity.Notice;
import com.lawchat.domain.notice.entity.NoticeCategory;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class NoticeDetailResponse {

    private final Long noticeId;
    private final NoticeCategory category;
    private final String title;
    private final String content;
    private final String fileUrl;
    private final Boolean isPinned;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private NoticeDetailResponse(Notice notice) {
        this.noticeId = notice.getNoticeId();
        this.category = notice.getCategory();
        this.title = notice.getTitle();
        this.content = notice.getContent();
        this.fileUrl = notice.getFileUrl();
        this.isPinned = notice.getIsPinned();
        this.createdAt = notice.getCreatedAt();
        this.updatedAt = notice.getUpdatedAt();
    }

    public static NoticeDetailResponse from(Notice notice) {
        return new NoticeDetailResponse(notice);
    }
}
