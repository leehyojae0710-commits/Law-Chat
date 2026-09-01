package com.lawchat.domain.notice.entity;

import com.lawchat.global.exception.BusinessException;
import com.lawchat.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 공지 팝업.
 *
 * link_url 컬럼은 DB에 존재하지만 의도적으로 매핑하지 않는다.
 * 팝업 클릭 시 페이지 이동 대신 목록에서 펼쳐 보는 방식으로 구현되어 URL 이동이 필요 없기 때문이다.
 * (매핑하지 않아도 INSERT 시 NULL 이 들어갈 뿐 오류는 나지 않는다)
 */
@Entity
@Getter
@Table(name = "notice_popups")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NoticePopup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "popup_id")
    private Long popupId;

    /**
     * 이 팝업을 만들어낸 공지. 공지 없이 만든 독립 배너면 null 이다.
     *
     * DB에 ON DELETE CASCADE 가 걸려 있지만 애플리케이션에서도 명시적으로 삭제한다.
     * JPA 는 DB 가 몰래 지운 행을 영속성 컨텍스트에서 알지 못하기 때문이다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notice_id")
    private Notice notice;

    @Column(name = "title", length = 255, nullable = false)
    private String title;

    @Column(name = "file_url", length = 500, nullable = false)
    private String fileUrl;

    @Column(name = "alt_text", length = 255)
    private String altText;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private NoticePopup(Notice notice, String title, String fileUrl, String altText,
                        LocalDateTime startDate, LocalDateTime endDate) {
        validatePeriod(startDate, endDate);
        this.notice = notice;
        this.title = title;
        this.fileUrl = fileUrl;
        this.altText = altText;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    /** 공지와 무관한 독립 팝업 (기존 API 유지용) */
    public static NoticePopup create(String title, String fileUrl, String altText,
                                     LocalDateTime startDate, LocalDateTime endDate) {
        return new NoticePopup(null, title, fileUrl, altText, startDate, endDate);
    }

    /** 공지에 연동된 팝업. 공지가 삭제되면 이 팝업도 함께 사라진다. */
    public static NoticePopup createForNotice(Notice notice, String title, String fileUrl,
                                              String altText, LocalDateTime startDate,
                                              LocalDateTime endDate) {
        return new NoticePopup(notice, title, fileUrl, altText, startDate, endDate);
    }

    /**
     * null 로 넘어온 필드는 변경하지 않는다. 기간은 둘 중 하나만 바뀌어도 최종 조합을 재검증.
     *
     * notice 는 여기서 바꾸지 않는다. 연동 대상 변경은 삭제 후 재등록으로 처리한다.
     */
    public void update(String title, String fileUrl, String altText,
                       LocalDateTime startDate, LocalDateTime endDate) {
        LocalDateTime newStart = (startDate != null) ? startDate : this.startDate;
        LocalDateTime newEnd = (endDate != null) ? endDate : this.endDate;
        validatePeriod(newStart, newEnd);

        if (title != null) {
            this.title = title;
        }
        if (fileUrl != null) {
            this.fileUrl = fileUrl;
        }
        if (altText != null) {
            this.altText = altText;
        }
        this.startDate = newStart;
        this.endDate = newEnd;
    }

    /** 공지 연동 팝업인지 여부. 관리자 화면에서 독립 배너와 구분해 표시할 때 쓴다. */
    public boolean isLinkedToNotice() {
        return notice != null;
    }

    /**
     * 지금 화면에 떠 있어야 하는 팝업인지 엔티티 스스로 판단.
     * 쿼리 조건과 별개로 서비스 레벨에서도 재사용 가능.
     */
    public boolean isActive(LocalDateTime now) {
        return !now.isBefore(startDate) && !now.isAfter(endDate);
    }

    /**
     * 종료일이 시작일보다 빠르면 영원히 노출되지 않는 팝업이 되므로 등록/수정 시점에 차단.
     */
    private void validatePeriod(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null || endDate == null || !startDate.isBefore(endDate)) {
            throw new BusinessException(ErrorCode.INVALID_POPUP_PERIOD);
        }
    }
}
