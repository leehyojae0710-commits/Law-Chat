package com.lawchat.domain.notice.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 부분 수정 허용: null 로 넘어온 필드는 Notice.update() 에서 변경하지 않음.
 */
@Getter
@NoArgsConstructor
public class NoticeUpdateRequest {

    private String title;
    private String content;
    private String fileUrl;
}
