package com.lawchat.domain.notice.service;

import com.lawchat.domain.notice.dto.request.NoticeCreateRequest;
import com.lawchat.domain.notice.dto.request.NoticeUpdateRequest;
import com.lawchat.domain.notice.dto.response.NoticeDetailResponse;
import com.lawchat.domain.notice.dto.response.NoticeListResponse;
import com.lawchat.domain.notice.entity.Notice;
import com.lawchat.domain.notice.entity.NoticeCategory;
import com.lawchat.domain.notice.entity.NoticePopup;
import com.lawchat.domain.notice.repository.NoticePopupRepository;
import com.lawchat.domain.notice.repository.NoticeRepository;
import com.lawchat.global.auth.AdminValidator;
import com.lawchat.global.exception.BusinessException;
import com.lawchat.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeService {

    /** 고정 공지 우선 -> 최신순. 클라이언트가 보낸 sort 파라미터는 무시하고 이 정렬로 통일. */
    private static final Sort NOTICE_SORT =
            Sort.by(Sort.Order.desc("isPinned"), Sort.Order.desc("createdAt"));

    private final NoticeRepository noticeRepository;
    private final NoticePopupRepository noticePopupRepository;
    private final AdminValidator adminValidator;

    public Page<NoticeListResponse> getNotices(NoticeCategory category, Pageable pageable) {
        Pageable fixedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                NOTICE_SORT
        );

        Page<Notice> notices = (category == null)
                ? noticeRepository.findAll(fixedPageable)
                : noticeRepository.findByCategory(category, fixedPageable);

        return notices.map(NoticeListResponse::from);
    }

    /**
     * 관리자 화면용 목록.
     * 현재는 공개 목록과 조건이 같지만, 별도 메서드로 분리해 두면
     * 나중에 "비공개 공지" 같은 관리자 전용 조건이 생겨도 공개 API 를 건드리지 않는다.
     */
    public Page<NoticeListResponse> getNoticesForAdmin(Long userId, NoticeCategory category, Pageable pageable) {
        adminValidator.validate(userId);
        return getNotices(category, pageable);
    }

    /**
     * 공지 상세. 연동 팝업 정보를 함께 내려준다.
     * 수정 화면에서 "팝업으로 노출하기" 체크박스와 노출 기간을 원래 상태로 복원하는 데 쓴다.
     */
    public NoticeDetailResponse getNotice(Long noticeId) {
        Notice notice = findNoticeOrThrow(noticeId);
        Optional<NoticePopup> popup = noticePopupRepository.findByNotice_NoticeId(noticeId);
        return NoticeDetailResponse.from(notice, popup.orElse(null));
    }

    /**
     * 공지 등록. 요청에 팝업 정보가 있으면 연동 팝업까지 한 트랜잭션에서 만든다.
     *
     * 공지 등록과 팝업 등록을 프론트에서 두 번 호출하면
     * 팝업이 어느 공지 소속인지 기록할 수 없고, 중간에 실패하면 한쪽만 남는다.
     */
    @Transactional
    public Long create(Long userId, NoticeCreateRequest request) {
        adminValidator.validate(userId);

        Notice notice = Notice.create(
                request.getCategory(),
                request.getTitle(),
                request.getContent(),
                request.getFileUrl()
        );
        Notice saved = noticeRepository.save(notice);

        if (request.isPopupRequested()) {
            createLinkedPopup(
                    saved,
                    request.getPopupTitle(),
                    request.getPopupFileUrl(),
                    request.getPopupAltText(),
                    request.getPopupStartDate(),
                    request.getPopupEndDate(),
                    request.getTitle(),
                    request.getFileUrl()
            );
        }
        return saved.getNoticeId();
    }

    /**
     * 공지 수정. 팝업 설정도 함께 반영한다.
     *
     *   createPopup 을 안 보냄 -> 팝업은 손대지 않음 (제목만 고치는 경우)
     *   createPopup = true     -> 팝업이 없으면 생성, 있으면 갱신
     *   createPopup = false    -> 연동 팝업 삭제 (체크박스 해제 = 팝업 끄기)
     */
    @Transactional
    public void update(Long userId, Long noticeId, NoticeUpdateRequest request) {
        adminValidator.validate(userId);

        Notice notice = findNoticeOrThrow(noticeId);
        notice.update(request.getTitle(), request.getContent(), request.getFileUrl());

        if (request.isPopupUntouched()) {
            return;
        }
        if (request.isPopupTurnedOff()) {
            noticePopupRepository.deleteByNotice_NoticeId(noticeId);
            return;
        }
        if (request.isPopupTurnedOn()) {
            applyPopupOn(notice, request);
        }
    }

    /**
     * 공지 삭제. 이 공지로 만들어진 팝업도 함께 삭제한다.
     *
     * DB에 ON DELETE CASCADE 가 걸려 있어도 애플리케이션에서 명시적으로 지운다.
     *  1. JPA 는 DB 가 몰래 지운 행을 영속성 컨텍스트에서 알지 못한다.
     *     같은 트랜잭션 안에서 이미 사라진 팝업을 다시 조회하면 유령 데이터가 보일 수 있다.
     *  2. CASCADE 없이 만들어진 로컬 DB 에서도 동일하게 동작한다.
     *
     * 순서가 중요하다. 팝업을 먼저 지우지 않으면 FK 제약에 걸린다.
     */
    @Transactional
    public void delete(Long userId, Long noticeId) {
        adminValidator.validate(userId);

        Notice notice = findNoticeOrThrow(noticeId);

        noticePopupRepository.deleteByNotice_NoticeId(noticeId);

        // 첨부파일 실물은 공유폴더에 남겨둔다 (잘못 지우면 복구 불가 - 필요 시 별도 배치로 정리)
        noticeRepository.delete(notice);
    }

    @Transactional
    public void togglePin(Long userId, Long noticeId) {
        adminValidator.validate(userId);

        findNoticeOrThrow(noticeId).togglePin();
    }

    // ------------------------------------------------------------------
    // 팝업 연동 내부 처리
    // ------------------------------------------------------------------

    /**
     * 체크박스를 켠 상태로 수정한 경우.
     * 기존 팝업이 있으면 갱신하고, 없으면 새로 만든다.
     */
    private void applyPopupOn(Notice notice, NoticeUpdateRequest request) {
        Optional<NoticePopup> existing =
                noticePopupRepository.findByNotice_NoticeId(notice.getNoticeId());

        if (existing.isPresent()) {
            // 기간을 안 보냈으면 기존 기간을 유지한다 (제목/이미지만 바꾸는 경우)
            existing.get().update(
                    request.getPopupTitle(),
                    request.getPopupFileUrl(),
                    request.getPopupAltText(),
                    request.getPopupStartDate(),
                    request.getPopupEndDate()
            );
            return;
        }

        // 새로 만드는 경우엔 기간이 반드시 필요하다
        if (request.getPopupStartDate() == null || request.getPopupEndDate() == null) {
            throw new BusinessException(ErrorCode.INVALID_POPUP_PERIOD);
        }
        createLinkedPopup(
                notice,
                request.getPopupTitle(),
                request.getPopupFileUrl(),
                request.getPopupAltText(),
                request.getPopupStartDate(),
                request.getPopupEndDate(),
                notice.getTitle(),
                notice.getFileUrl()
        );
    }

    /**
     * 연동 팝업 생성.
     * 팝업 제목/이미지를 따로 안 보내면 공지의 값을 그대로 쓴다.
     * (관리자가 팝업용으로 따로 준비하지 않는 경우가 대부분이다)
     */
    private void createLinkedPopup(Notice notice, String popupTitle, String popupFileUrl,
                                   String popupAltText, LocalDateTime startDate,
                                   LocalDateTime endDate, String fallbackTitle,
                                   String fallbackFileUrl) {
        String title = (popupTitle != null && !popupTitle.isBlank()) ? popupTitle : fallbackTitle;
        String fileUrl = (popupFileUrl != null && !popupFileUrl.isBlank())
                ? popupFileUrl
                : fallbackFileUrl;

        // 팝업은 이미지가 필수다. 공지 첨부도 없이 팝업만 요청하면 잘못된 입력.
        if (fileUrl == null || fileUrl.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_FILE);
        }

        noticePopupRepository.save(
                NoticePopup.createForNotice(notice, title, fileUrl, popupAltText, startDate, endDate)
        );
    }

    private Notice findNoticeOrThrow(Long noticeId) {
        return noticeRepository.findById(noticeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTICE_NOT_FOUND));
    }
}
