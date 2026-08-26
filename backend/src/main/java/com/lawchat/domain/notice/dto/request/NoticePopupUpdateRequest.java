package com.lawchat.domain.notice.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 부분 수정 허용. 노출 기간 연장이 가장 흔한 케이스라 startDate/endDate 도 개별 수정 가능.
 * 기간 유효성(start < end)은 NoticePopup.update() 에서 최종 조합으로 재검증한다.
 */
@Getter
@NoArgsConstructor
public class NoticePopupUpdateRequest {

    private String title;
    private String fileUrl;
    private String linkUrl;
    private String altText;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}
