package com.lawchat.domain.notice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class NoticePopupCreateRequest {

    @NotBlank(message = "제목은 필수입니다.")
    private String title;

    @NotBlank(message = "이미지 경로는 필수입니다.")
    private String fileUrl;

    private String altText;

    @NotNull(message = "노출 시작 일시는 필수입니다.")
    private LocalDateTime startDate;

    @NotNull(message = "노출 종료 일시는 필수입니다.")
    private LocalDateTime endDate;
}
