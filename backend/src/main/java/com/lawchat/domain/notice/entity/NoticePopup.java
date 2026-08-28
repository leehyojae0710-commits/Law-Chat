package com.lawchat.domain.notice.entity;

import com.lawchat.global.exception.BusinessException;
import com.lawchat.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "notice_popups")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NoticePopup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "popup_id")
    private Long popupId;

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

    private NoticePopup(String title, String fileUrl, String altText,
                         LocalDateTime startDate, LocalDateTime endDate) {
        validatePeriod(startDate, endDate);
        this.title = title;
        this.fileUrl = fileUrl;
        this.altText = altText;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public static NoticePopup create(String title, String fileUrl, String altText,
                                      LocalDateTime startDate, LocalDateTime endDate) {
        return new NoticePopup(title, fileUrl, altText, startDate, endDate);
    }

    /**
     * null 로 넘어온 필드는 변경하지 않는다. 기간은 둘 중 하나만 바뀌어도 최종 조합을 재검증.
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
