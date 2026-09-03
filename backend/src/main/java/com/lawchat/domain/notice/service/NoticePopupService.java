package com.lawchat.domain.notice.service;

import com.lawchat.domain.notice.dto.request.NoticePopupCreateRequest;
import com.lawchat.domain.notice.dto.request.NoticePopupUpdateRequest;
import com.lawchat.domain.notice.dto.response.NoticePopupAdminResponse;
import com.lawchat.domain.notice.dto.response.NoticePopupResponse;
import com.lawchat.domain.notice.entity.NoticePopup;
import com.lawchat.domain.notice.repository.NoticePopupRepository;
import com.lawchat.global.auth.AdminValidator;
import com.lawchat.global.exception.BusinessException;
import com.lawchat.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticePopupService {

    private final NoticePopupRepository noticePopupRepository;
    private final AdminValidator adminValidator;

    /** 사용자 화면용 - 지금 노출 기간에 걸린 팝업만 */
    public List<NoticePopupResponse> getActivePopups() {
        return noticePopupRepository.findActivePopups(LocalDateTime.now()).stream()
                .map(NoticePopupResponse::from)
                .toList();
    }

    /** 관리자 화면용 - 기간 지난 것 포함 전체 (활성 여부 플래그 동봉) */
    public List<NoticePopupAdminResponse> getAllPopups(Long userId) {
        adminValidator.validate(userId);

        LocalDateTime now = LocalDateTime.now();
        return noticePopupRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(popup -> NoticePopupAdminResponse.from(popup, now))
                .toList();
    }

    @Transactional
    public Long create(Long userId, NoticePopupCreateRequest request) {
        adminValidator.validate(userId);

        NoticePopup popup = NoticePopup.create(
                request.getTitle(),
                request.getFileUrl(),
                request.getAltText(),
                request.getStartDate(),
                request.getEndDate()
        );
        return noticePopupRepository.save(popup).getPopupId();
    }

    @Transactional
    public void update(Long userId, Long popupId, NoticePopupUpdateRequest request) {
        adminValidator.validate(userId);

        NoticePopup popup = findPopupOrThrow(popupId);
        popup.update(
                request.getTitle(),
                request.getFileUrl(),
                request.getAltText(),
                request.getStartDate(),
                request.getEndDate()
        );
    }

    @Transactional
    public void delete(Long userId, Long popupId) {
        adminValidator.validate(userId);

        noticePopupRepository.delete(findPopupOrThrow(popupId));
    }

    private NoticePopup findPopupOrThrow(Long popupId) {
        return noticePopupRepository.findById(popupId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POPUP_NOT_FOUND));
    }
}
