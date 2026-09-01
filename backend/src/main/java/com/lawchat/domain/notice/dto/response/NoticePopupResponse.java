package com.lawchat.domain.notice.dto.response;

import com.lawchat.domain.notice.entity.NoticePopup;
import com.lawchat.global.file.FileUrls;
import lombok.Getter;

/**
 * 사용자 화면용 - 렌더링에 필요한 최소 필드만.
 * fileUrl 은 <img src> 에 그대로 넣을 수 있는 절대 URL 이다.
 */
@Getter
public class NoticePopupResponse {

    private final Long popupId;
    private final String title;
    private final String fileUrl;
    private final String altText;

    private NoticePopupResponse(NoticePopup popup) {
        this.popupId = popup.getPopupId();
        this.title = popup.getTitle();
        this.fileUrl = FileUrls.view(popup.getFileUrl());
        this.altText = popup.getAltText();
    }

    public static NoticePopupResponse from(NoticePopup popup) {
        return new NoticePopupResponse(popup);
    }
}
