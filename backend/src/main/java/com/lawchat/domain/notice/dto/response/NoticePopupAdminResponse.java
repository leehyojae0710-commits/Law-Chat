package com.lawchat.domain.notice.dto.response;

import com.lawchat.domain.notice.entity.NoticePopup;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 관리자 화면용 - 노출 기간과 현재 활성 여부까지 함께 내려준다.
 * (등록해둔 팝업을 확인하고 기간 연장/삭제 판단을 하기 위함)
 */
@Getter
public class NoticePopupAdminResponse {

    private final Long popupId;
    private final String title;
    private final String fileUrl;
    private final String linkUrl;
    private final String altText;
    private final LocalDateTime startDate;
    private final LocalDateTime endDate;
    private final LocalDateTime createdAt;
    private final Boolean isActive;

    private NoticePopupAdminResponse(NoticePopup popup, LocalDateTime now) {
        this.popupId = popup.getPopupId();
        this.title = popup.getTitle();
        this.fileUrl = popup.getFileUrl();
        this.linkUrl = popup.getLinkUrl();
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
