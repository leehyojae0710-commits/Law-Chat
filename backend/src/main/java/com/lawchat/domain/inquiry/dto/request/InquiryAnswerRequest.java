package com.lawchat.domain.inquiry.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class InquiryAnswerRequest {

    @NotBlank(message = "답변 내용은 필수입니다.")
    @Size(max = 4000, message = "답변은 4000자까지 입력할 수 있습니다.")
    private String answerContent;
}
