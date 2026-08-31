package com.lawchat.domain.notice.dto.request;

import com.lawchat.domain.notice.entity.NoticeCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 공지 등록 요청.
 *
 * "이 공지를 팝업으로도 노출하기" 체크박스를 켜면 popup* 필드가 함께 온다.
 * 공지 등록과 팝업 등록을 두 번 호출하지 않고 한 요청으로 처리해야
 * 팝업이 어느 공지 소속인지 기록할 수 있고, 중간 실패 시 함께 롤백된다.
 *
 * 팝업 제목과 이미지는 받지 않는다. 관리자 화면에 별도 입력란이 없고
 * 공지의 제목·첨부 이미지를 그대로 쓰기 때문이다. (NoticeService 가 채운다)
 */
@Getter
@NoArgsConstructor
public class NoticeCreateRequest {

    @NotNull(message = "카테고리는 필수입니다.")
    private NoticeCategory category;

    @NotBlank(message = "제목은 필수입니다.")
    private String title;

    @NotBlank(message = "내용은 필수입니다.")
    private String content;

    /** /api/admin/notices/upload 응답으로 받은 파일명 */
    private String fileUrl;

    // ---------- 팝업 동시 등록용 (선택) ----------

    /** true 일 때만 팝업을 함께 만든다. 체크박스와 1:1 대응. */
    private boolean createPopup;

    private LocalDateTime popupStartDate;

    private LocalDateTime popupEndDate;

    /**
     * 팝업을 만들어야 하는 요청인지 판단.
     * 체크만 켜고 기간을 안 보내면 팝업을 만들 수 없으므로 여기서 함께 확인한다.
     * (기간의 앞뒤 순서는 NoticePopup 생성자가 최종 검증한다)
     */
    public boolean isPopupRequested() {
        return createPopup && popupStartDate != null && popupEndDate != null;
    }
}
