package com.lawchat.domain.inquiry.dto.request;

import com.lawchat.domain.inquiry.entity.InquiryCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class InquiryCreateRequest {

    @NotNull(message = "문의 유형은 필수입니다.")
    private InquiryCategory category;

    @NotBlank(message = "제목은 필수입니다.")
    @Size(min = 2, max = 100, message = "제목은 2~100자로 입력해 주세요.")
    private String title;

    @NotBlank(message = "문의 내용은 필수입니다.")
    @Size(min = 10, max = 2000, message = "문의 내용은 10~2000자로 입력해 주세요.")
    private String content;

    /** /api/inquiries/upload 응답으로 받은 파일명 (절대 URL 이 아니라 fileName) */
    @Size(max = 512)
    private String screenshotUrl;
}
