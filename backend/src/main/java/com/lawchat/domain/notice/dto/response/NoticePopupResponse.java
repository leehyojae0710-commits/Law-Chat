package com.lawchat.domain.notice.dto.response;

import com.lawchat.domain.notice.entity.NoticePopup;
import lombok.Getter;

/** 사용자 화면용 - 렌더링에 필요한 최소 필드만 */
@Getter
public class NoticePopupResponse {

    private final Long popupId;
    private final String title;
    private final String fileUrl;
    private final String linkUrl;
    private final String altText;

    private NoticePopupResponse(NoticePopup popup) {
        this.popupId = popup.getPopupId();
        this.title = popup.getTitle();
        this.fileUrl = popup.getFileUrl();
        this.linkUrl = popup.getLinkUrl();
        this.altText = popup.getAltText();
    }

    public static NoticePopupResponse from(NoticePopup popup) {
        return new NoticePopupResponse(popup);
    }
}
