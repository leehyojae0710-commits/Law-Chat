package com.lawchat.domain.user.dto.response;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 이메일·전화번호 중복검사 결과.
 *
 * ★ 왜 한 번에 세 가지를 알려주는가
 *   가입 화면에서 「중복확인」을 누르면 세 가지 중 하나다.
 *     1) 쓸 수 있음
 *     2) 이미 다른 사람이 쓰는 중
 *     3) 내가 예전에 탈퇴한 계정  ← 이 경우 복구를 안내해야 한다
 *   예전에는 UNIQUE 제약에 걸려 가입 실패로만 알 수 있었고,
 *   3번인데도 "이미 사용 중" 으로 읽혀 되찾을 방법을 안내받지 못했다.
 *
 * ★ 왜 status 를 문자열로 주는가
 *   available(boolean) 하나로는 2번과 3번을 구분할 수 없다.
 *   화면이 다른 안내를 띄워야 하므로 상태를 명시한다.
 *
 * ★ 왜 남은 기간을 계산하지 않는가
 *   "며칠 뒤면 복구할 수 없다" 고 알리면 사용자를 재촉하게 되고,
 *   경계 근처에서는 오차 하루로 안내와 실제가 어긋난다.
 *   복구할 수 있는지(restorable)와 언제 탈퇴했는지(deletedAt)만 알려주면 충분하다.
 *
 * @param available     이 값으로 가입할 수 있는지 (탈퇴 계정이면 false — 복구를 먼저 해야 한다)
 * @param status        AVAILABLE / IN_USE / WITHDRAWN
 * @param deletedAt     탈퇴 시각 (WITHDRAWN 일 때만)
 * @param deletedAtText 화면에 그대로 쓸 수 있는 탈퇴일 문구 (예: "2026년 9월 1일")
 * @param maskedEmail   복구 대상 계정의 가려진 이메일 (전화번호로 조회했을 때 어느 계정인지 확인용)
 * @param restorable    이 화면에서 복구할 수 있는지 (소셜 가입자는 false)
 * @param verifyBy      복구에 쓸 인증 수단. EMAIL / PHONE / null
 *                      화면이 어느 인증 화면으로 보낼지 결정할 때 쓴다.
 * @param verifyTarget  인증코드를 받을 연락처 (조회에 쓴 값 그대로)
 * @param message       화면에 그대로 띄울 수 있는 안내 문구
 */
public record AvailabilityResponse(
        boolean available,
        String status,
        LocalDateTime deletedAt,
        String deletedAtText,
        String maskedEmail,
        boolean restorable,
        String verifyBy,
        String verifyTarget,
        String message
) {

    private static final DateTimeFormatter KOREAN_DATE =
            DateTimeFormatter.ofPattern("yyyy년 M월 d일");

    /**
     * 사용 가능.
     *
     * 메서드 이름이 ok 인 이유 — record 컴포넌트 available 이 같은 이름의 접근자
     * available() 을 자동 생성한다. 같은 이름의 static 메서드를 두면
     * "return type of accessor method must match" 컴파일 오류가 난다.
     */
    public static AvailabilityResponse ok() {
        return new AvailabilityResponse(true, "AVAILABLE", null, null, null, false, null, null,
                "사용할 수 있어요.");
    }

    /** 이미 다른 사람이 사용 중. */
    public static AvailabilityResponse inUse() {
        return new AvailabilityResponse(false, "IN_USE", null, null, null, false, null, null,
                "이미 사용 중이에요.");
    }

    /**
     * 탈퇴 계정 — 복구 가능.
     *
     * message 는 화면이 그대로 띄울 수 있게 완성된 문장으로 만든다.
     * 화면마다 문구를 조립하면 같은 상황에 다른 안내가 나간다.
     */
    /**
     * 탈퇴 계정 — 복구 가능.
     *
     * 메서드 이름에 withdrawn 을 쓴 이유 — record 컴포넌트 restorable 이
     * 같은 이름의 접근자를 자동 생성해 충돌한다. (available 도 같은 이유로 ok 로 둔다)
     */
    public static AvailabilityResponse withdrawnRestorable(LocalDateTime deletedAt, String maskedEmail,
                                                  String verifyBy, String verifyTarget) {
        String dateText = format(deletedAt);
        boolean byEmail = "EMAIL".equals(verifyBy);

        // 전화번호로 조회한 경우에만 어느 계정인지 알려준다.
        //   이메일로 조회했으면 사용자가 이미 그 주소를 알고 있어 덧붙일 필요가 없다.
        //   전화번호만 입력한 사용자는 그 번호에 어떤 계정이 묶여 있는지 모른다.
        String head = (!byEmail && maskedEmail != null)
                ? maskedEmail + " 계정으로 가입한 이력이 있습니다. "
                : "복구가 가능한 상태입니다. ";

        return new AvailabilityResponse(
                false, "WITHDRAWN", deletedAt, dateText, maskedEmail, true, verifyBy, verifyTarget,
                head
                        + (dateText != null ? "탈퇴일은 " + dateText + "입니다. " : "")
                        + (byEmail ? "이메일" : "전화번호") + " 인증으로 복구하시겠습니까?");
    }

    /** 탈퇴 계정이지만 이 화면에서 복구할 수 없음 (소셜 가입자 등) */
    public static AvailabilityResponse withdrawnNotRestorable(LocalDateTime deletedAt, String maskedEmail,
                                                     String message) {
        return new AvailabilityResponse(
                false, "WITHDRAWN", deletedAt, format(deletedAt), maskedEmail, false, null, null,
                message);
    }

    private static String format(LocalDateTime at) {
        return (at == null) ? null : at.format(KOREAN_DATE);
    }
}
