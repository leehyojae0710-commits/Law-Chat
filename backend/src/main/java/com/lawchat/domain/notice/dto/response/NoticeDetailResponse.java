package com.lawchat.domain.notice.dto.response;

import com.lawchat.domain.notice.entity.Notice;
import com.lawchat.domain.notice.entity.NoticeCategory;
import com.lawchat.domain.notice.entity.NoticePopup;
import com.lawchat.global.file.FileUrls;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 상세 응답.
 *  - fileUrl  : 브라우저가 바로 쓸 수 있는 다운로드 절대 URL (첨부 없으면 null)
 *  - fileName : DB에 저장된 원본 파일명. 수정 화면에서 "기존 첨부 유지" 표시용.
 *
 * 팝업 필드는 수정 화면에서 체크박스와 노출 기간을 원래 상태로 복원하는 데 쓴다.
 * 연동 팝업이 없으면 hasPopup = false 이고 나머지 팝업 필드는 모두 null 이다.
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

    // ---------- 연동 팝업 ----------
    private final Boolean hasPopup;
    private final Long popupId;
    private final String popupTitle;
    private final String popupFileUrl;
    private final String popupFileName;
    private final String popupAltText;
    private final LocalDateTime popupStartDate;
    private final LocalDateTime popupEndDate;

    private NoticeDetailResponse(Notice notice, NoticePopup popup) {
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

        this.hasPopup = (popup != null);
        this.popupId = (popup != null) ? popup.getPopupId() : null;
        this.popupTitle = (popup != null) ? popup.getTitle() : null;
        // 팝업 이미지는 화면에 바로 띄워야 하므로 download 가 아니라 view URL
        this.popupFileUrl = (popup != null) ? FileUrls.view(popup.getFileUrl()) : null;
        this.popupFileName = (popup != null) ? popup.getFileUrl() : null;
        this.popupAltText = (popup != null) ? popup.getAltText() : null;
        this.popupStartDate = (popup != null) ? popup.getStartDate() : null;
        this.popupEndDate = (popup != null) ? popup.getEndDate() : null;
    }

    public static NoticeDetailResponse from(Notice notice, NoticePopup popup) {
        return new NoticeDetailResponse(notice, popup);
    }

    /** 팝업 정보가 필요 없는 곳에서 쓰는 오버로드 (기존 호출부 호환) */
    public static NoticeDetailResponse from(Notice notice) {
        return new NoticeDetailResponse(notice, null);
    }
}
