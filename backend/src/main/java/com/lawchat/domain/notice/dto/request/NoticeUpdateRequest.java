package com.lawchat.domain.notice.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 부분 수정 허용: null 로 넘어온 필드는 Notice.update() 에서 변경하지 않음.
 *
 * 팝업은 다르게 동작한다. createPopup 은 Boolean(래퍼)이라 세 가지 상태를 구분한다.
 *   null   -> 팝업 설정을 건드리지 않음 (제목만 고치는 경우 등)
 *   TRUE   -> 팝업 생성 또는 기존 팝업 갱신
 *   FALSE  -> 연동된 팝업 삭제 (= 체크박스 해제)
 *
 * primitive boolean 으로 두면 "안 보냄"과 "false 로 보냄"을 구분할 수 없어
 * 제목만 수정해도 팝업이 꺼져버린다. 그래서 여기서만 Boolean 을 쓴다.
 */
@Getter
@NoArgsConstructor
public class NoticeUpdateRequest {

    private String title;
    private String content;
    private String fileUrl;

    // ---------- 팝업 설정 ----------

    private Boolean createPopup;

    private String popupTitle;

    private String popupFileUrl;

    private String popupAltText;

    private LocalDateTime popupStartDate;

    private LocalDateTime popupEndDate;

    /** 팝업 설정을 아예 보내지 않은 요청인지 */
    public boolean isPopupUntouched() {
        return createPopup == null;
    }

    /** 체크박스를 해제한 요청인지 (연동 팝업을 지워야 함) */
    public boolean isPopupTurnedOff() {
        return Boolean.FALSE.equals(createPopup);
    }

    /** 체크박스를 켠 요청인지 (생성 또는 갱신) */
    public boolean isPopupTurnedOn() {
        return Boolean.TRUE.equals(createPopup);
    }
}
