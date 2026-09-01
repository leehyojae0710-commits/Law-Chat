package com.lawchat.domain.inquiry.service;

import com.lawchat.domain.inquiry.dto.request.InquiryAnswerRequest;
import com.lawchat.domain.inquiry.dto.request.InquiryCreateRequest;
import com.lawchat.domain.inquiry.dto.response.InquiryAdminResponse;
import com.lawchat.domain.inquiry.dto.response.InquiryDetailResponse;
import com.lawchat.domain.inquiry.dto.response.InquirySummaryResponse;
import com.lawchat.domain.inquiry.entity.Inquiry;
import com.lawchat.domain.inquiry.entity.InquiryCategory;
import com.lawchat.domain.inquiry.entity.InquiryStatus;
import com.lawchat.domain.inquiry.repository.InquiryRepository;
import com.lawchat.domain.user.entity.User;
import com.lawchat.domain.user.repository.UserRepository;
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
public class InquiryService {

    /** 최신순 고정. 클라이언트가 보낸 sort 파라미터는 무시한다. (NoticeService 와 동일한 방침) */
    private static final Sort INQUIRY_SORT = Sort.by(Sort.Order.desc("createdAt"));

    private final InquiryRepository inquiryRepository;
    private final UserRepository userRepository;
    private final AdminValidator adminValidator;

    // ------------------------------------------------------------------
    // 사용자
    // ------------------------------------------------------------------

    @Transactional
    public Long create(Long userId, InquiryCreateRequest request) {
        User user = findUserOrThrow(userId);

        Inquiry inquiry = Inquiry.create(
                user,
                request.getCategory(),
                request.getTitle().trim(),
                request.getContent().trim(),
                request.getScreenshotUrl()
        );
        return inquiryRepository.save(inquiry).getInquiryId();
    }

    public Page<InquirySummaryResponse> getMyInquiries(Long userId, Pageable pageable) {
        Pageable fixedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                INQUIRY_SORT
        );
        return inquiryRepository.findByUser_UserId(userId, fixedPageable)
                .map(InquirySummaryResponse::from);
    }

    public InquiryDetailResponse getMyInquiry(Long userId, Long inquiryId) {
        return InquiryDetailResponse.from(findOwnedInquiryOrThrow(userId, inquiryId));
    }

    /**
     * 답변이 달리기 전에만 삭제할 수 있다.
     * 이미 답변한 문의를 지우면 관리자의 응대 이력까지 사라지기 때문이다.
     */
    @Transactional
    public void delete(Long userId, Long inquiryId) {
        Inquiry inquiry = findOwnedInquiryOrThrow(userId, inquiryId);

        if (!inquiry.isDeletable()) {
            throw new BusinessException(ErrorCode.INQUIRY_ALREADY_ANSWERED);
        }
        inquiryRepository.delete(inquiry);
    }

    // ------------------------------------------------------------------
    // 관리자
    // ------------------------------------------------------------------

    public Page<InquiryAdminResponse> getInquiriesForAdmin(Long userId,
                                                           InquiryStatus status,
                                                           InquiryCategory category,
                                                           Pageable pageable) {
        adminValidator.validate(userId);

        Boolean answeredOnly = (status == null) ? null : (status == InquiryStatus.ANSWERED);
        Pageable fixedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                INQUIRY_SORT
        );

        return inquiryRepository.searchForAdmin(answeredOnly, category, fixedPageable)
                .map(InquiryAdminResponse::from);
    }

    public InquiryAdminResponse getInquiryForAdmin(Long userId, Long inquiryId) {
        adminValidator.validate(userId);
        Inquiry inquiry = inquiryRepository.findWithUserByInquiryId(inquiryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INQUIRY_NOT_FOUND));
        return InquiryAdminResponse.from(inquiry);
    }

    /**
     * 답변 등록 · 수정. 같은 메서드로 둘 다 처리한다.
     * 등록되는 즉시 사용자 화면에서 답변완료로 바뀐다.
     */
    @Transactional
    public void answer(Long adminUserId, Long inquiryId, InquiryAnswerRequest request) {
        adminValidator.validate(adminUserId);

        Inquiry inquiry = findInquiryOrThrow(inquiryId);
        User admin = findUserOrThrow(adminUserId);

        inquiry.answer(admin, request.getAnswerContent().trim());
        // 영속 상태 엔티티라 변경 감지로 UPDATE 가 나간다. save() 호출 불필요.
    }

    /** 관리자 대시보드용 미답변 건수 */
    public long countPending() {
        return inquiryRepository.countByAnsweredAtIsNull();
    }

    // ------------------------------------------------------------------
    // 내부 헬퍼
    // ------------------------------------------------------------------

    private Inquiry findInquiryOrThrow(Long inquiryId) {
        return inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INQUIRY_NOT_FOUND));
    }

    private Inquiry findOwnedInquiryOrThrow(Long userId, Long inquiryId) {
        Inquiry inquiry = findInquiryOrThrow(inquiryId);

        if (!inquiry.isOwnedBy(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        return inquiry;
    }

    private User findUserOrThrow(Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
