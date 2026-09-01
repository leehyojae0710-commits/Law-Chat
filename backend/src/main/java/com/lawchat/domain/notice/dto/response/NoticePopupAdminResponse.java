package com.lawchat.domain.notice.dto.response;

import com.lawchat.domain.notice.entity.NoticePopup;
import com.lawchat.global.file.FileUrls;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 관리자 화면용 - 노출 기간과 현재 활성 여부까지 함께 내려준다.
 *  - fileUrl : 미리보기용 절대 URL
 *  - fileName : 수정 요청 시 그대로 되돌려 보낼 원본 파일명
 *  - noticeId / linkedToNotice : 연동된 공지. 이 값이 있는 팝업은 공지를 지우면 함께 사라진다.
 */
@Getter
public class NoticePopupAdminResponse {

    private final Long popupId;
    private final Long noticeId;
    private final Boolean linkedToNotice;
    private final String title;
    private final String fileUrl;
    private final String fileName;
    private final String altText;
    private final LocalDateTime startDate;
    private final LocalDateTime endDate;
    private final LocalDateTime createdAt;
    private final Boolean isActive;

    private NoticePopupAdminResponse(NoticePopup popup, LocalDateTime now) {
        this.popupId = popup.getPopupId();
        this.noticeId = popup.isLinkedToNotice() ? popup.getNotice().getNoticeId() : null;
        this.linkedToNotice = popup.isLinkedToNotice();
        this.title = popup.getTitle();
        this.fileUrl = FileUrls.view(popup.getFileUrl());
        this.fileName = popup.getFileUrl();
        this.altText = popup.getAltText();
        this.startDate = popup.getStartDate();
        this.endDate = popup.getEndDate();
        this.createdAt = popup.getCreatedAt();
        this.isActive = popup.isActive(now);
    }

    public static NoticePopupAdminResponse from(NoticePopup popup, LocalDateTime now) {
        return new NoticePopupAdminResponse(popup, now);
    }
}
