package com.lawchat.domain.notice.dto.response;

import com.lawchat.domain.notice.entity.Notice;
import com.lawchat.domain.notice.entity.NoticeCategory;
import com.lawchat.global.file.FileUrls;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 상세 응답.
 *  - fileUrl  : 브라우저가 바로 쓸 수 있는 다운로드 절대 URL (첨부 없으면 null)
 *  - fileName : DB에 저장된 원본 파일명. 수정 화면에서 "기존 첨부 유지" 표시용.
 */
@Getter
public class NoticeDetailResponse {

    private final Long noticeId;
    private final NoticeCategory category;
    private final String categoryLabel;
    private final String title;
    private final String content;
    private final String fileUrl;
    private final String fileName;
    private final Boolean isPinned;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private NoticeDetailResponse(Notice notice) {
        this.noticeId = notice.getNoticeId();
        this.category = notice.getCategory();
        this.categoryLabel = notice.getCategory().getLabel();
        this.title = notice.getTitle();
        this.content = notice.getContent();
        this.fileUrl = FileUrls.download(notice.getFileUrl());
        this.fileName = notice.getFileUrl();
        this.isPinned = notice.getIsPinned();
        this.createdAt = notice.getCreatedAt();
        this.updatedAt = notice.getUpdatedAt();
    }

    public static NoticeDetailResponse from(Notice notice) {
        return new NoticeDetailResponse(notice);
    }
}
