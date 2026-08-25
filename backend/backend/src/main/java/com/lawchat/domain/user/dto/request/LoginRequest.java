package com.lawchat.domain.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 로그인 요청 바디.
 *
 * 여기서는 비밀번호 형식 검증(@Pattern)을 걸지 않는다.
 * 로그인은 "형식이 맞는지"가 아니라 "DB 값과 일치하는지"만 따지면 되고,
 * 형식 오류 메시지를 따로 주면 공격자에게 비밀번호 정책 힌트를 주게 된다.
 */
public record LoginRequest(

        @NotBlank(message = "이메일을 입력해 주세요.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        String email,

        @NotBlank(message = "비밀번호를 입력해 주세요.")
        String password
) {
}
