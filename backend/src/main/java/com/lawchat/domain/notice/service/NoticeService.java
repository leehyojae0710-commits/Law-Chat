package com.lawchat.domain.notice.service;

import com.lawchat.domain.notice.dto.request.NoticeCreateRequest;
import com.lawchat.domain.notice.dto.request.NoticeUpdateRequest;
import com.lawchat.domain.notice.dto.response.NoticeDetailResponse;
import com.lawchat.domain.notice.dto.response.NoticeListResponse;
import com.lawchat.domain.notice.entity.Notice;
import com.lawchat.domain.notice.entity.NoticeCategory;
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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeService {

    /** 고정 공지 우선 → 최신순. 클라이언트가 보낸 sort 파라미터는 무시하고 이 정렬로 통일. */
    private static final Sort NOTICE_SORT =
            Sort.by(Sort.Order.desc("isPinned"), Sort.Order.desc("createdAt"));

    private final NoticeRepository noticeRepository;
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

    public NoticeDetailResponse getNotice(Long noticeId) {
        return NoticeDetailResponse.from(findNoticeOrThrow(noticeId));
    }

    @Transactional
    public Long create(Long userId, NoticeCreateRequest request) {
        adminValidator.validate(userId);

        Notice notice = Notice.create(
                request.getCategory(),
                request.getTitle(),
                request.getContent(),
                request.getFileUrl()
        );
        return noticeRepository.save(notice).getNoticeId();
    }

    @Transactional
    public void update(Long userId, Long noticeId, NoticeUpdateRequest request) {
        adminValidator.validate(userId);

        Notice notice = findNoticeOrThrow(noticeId);
        notice.update(request.getTitle(), request.getContent(), request.getFileUrl());
    }

    @Transactional
    public void delete(Long userId, Long noticeId) {
        adminValidator.validate(userId);

        // 첨부파일 실물은 공유폴더에 남겨둔다 (잘못 지우면 복구 불가 — 필요 시 별도 배치로 정리)
        noticeRepository.delete(findNoticeOrThrow(noticeId));
    }

    @Transactional
    public void togglePin(Long userId, Long noticeId) {
        adminValidator.validate(userId);

        findNoticeOrThrow(noticeId).togglePin();
    }

    private Notice findNoticeOrThrow(Long noticeId) {
        return noticeRepository.findById(noticeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTICE_NOT_FOUND));
    }
}
