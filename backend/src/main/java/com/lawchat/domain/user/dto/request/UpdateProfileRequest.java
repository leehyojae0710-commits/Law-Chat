package com.lawchat.domain.user.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 프로필 수정 요청 (PATCH /api/users/me).
 *
 * ★ 모든 필드가 optional 이다 — 부분 수정(PATCH)이기 때문.
 *   프론트는 사용자가 실제로 바꾼 필드만 담아 보내면 되고,
 *   보내지 않은 필드는 Jackson 이 null 로 채워 서비스 계층에서 "변경 없음"으로 처리된다.
 *
 * ★ @NotBlank 를 쓰지 않은 이유
 *   @NotBlank 를 붙이면 "필드를 안 보낸 경우"까지 검증 실패로 처리되어
 *   부분 수정 자체가 불가능해진다. 반면 @Size / @Pattern 은 값이 null 이면
 *   검사를 건너뛰므로, "보냈다면 형식을 지켜라"는 의도에 정확히 맞는다.
 *
 * ★ 검증 규칙은 SignupRequest 와 동일하게 맞췄다.
 *   가입 때는 2자 이상이어야 하는데 수정 때는 1자도 통과된다면 규칙이 뚫리는 셈이고,
 *   nickname 은 DB 가 varchar(50) 이라 50자를 넘기면 DB 레벨에서 500 이 터진다.
 *   여기서 400 으로 걸러 사용자에게 이유를 알려주는 편이 낫다.
 */
public record UpdateProfileRequest(

        @Size(min = 2, max = 50, message = "닉네임은 2자 이상 50자 이하여야 합니다.")
        String nickname,

        @Size(max = 512, message = "프로필 이미지 주소가 너무 깁니다.")
        String profileImg,

        /**
         * 하이픈 포함/미포함 모두 허용한다. 서비스 계층에서 숫자만 남기도록 정규화하므로
         * 여기서는 "숫자와 하이픈으로만 이루어졌는지" 정도만 본다.
         * DB 컬럼이 varchar(20) 이라 상한도 20 으로 맞춘다.
         */
        @Pattern(
                regexp = "^[0-9-]{9,20}$",
                message = "전화번호는 숫자와 하이픈만 사용할 수 있습니다."
        )
        String phone
) {
}
