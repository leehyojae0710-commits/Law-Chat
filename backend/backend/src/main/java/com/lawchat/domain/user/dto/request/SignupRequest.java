package com.lawchat.domain.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 회원가입 요청 바디.
 *
 * record 를 쓰면 필드/생성자/getter/equals/toString 이 자동 생성되어
 * 값만 담아 나르는 DTO 에 딱 맞는다. (Java 16+)
 *
 * jakarta.validation 애노테이션은 컨트롤러에서 @Valid 를 붙였을 때 동작한다.
 * 검증에 실패하면 MethodArgumentNotValidException 이 발생하고,
 * GlobalExceptionHandler 가 이를 잡아 400 응답으로 변환한다.
 * → 서비스 코드에 "값이 비었는지" 검사 코드를 쓰지 않아도 된다.
 */
public record SignupRequest(

        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        @Size(max = 255)
        String email,

        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(min = 8, max = 64, message = "비밀번호는 8자 이상 64자 이하여야 합니다.")
        // 영문/숫자/특수문자 중 2종류 이상 조합을 강제하는 정규식
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,64}$",
                message = "비밀번호는 영문과 숫자를 모두 포함해야 합니다."
        )
        String password,

        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(min = 2, max = 50, message = "닉네임은 2자 이상 50자 이하여야 합니다.")
        String nickname
) {
}
