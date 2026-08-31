package com.lawchat.domain.inquiry.controller;

import com.lawchat.domain.inquiry.dto.request.InquiryAnswerRequest;
import com.lawchat.domain.inquiry.dto.response.InquiryAdminResponse;
import com.lawchat.domain.inquiry.entity.InquiryCategory;
import com.lawchat.domain.inquiry.entity.InquiryStatus;
import com.lawchat.domain.inquiry.service.InquiryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 관리자 전용 1:1 문의 API.
 * 권한 검증은 AdminNoticeController 와 동일하게 서비스 메서드 내부에서 AdminValidator 로 수행한다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/inquiries")
public class AdminInquiryController {

    private final InquiryService inquiryService;

    /**
     * status / category 는 생략 가능(생략 시 전체).
     * status 는 DB 컬럼이 아니라 파생 값이지만, 서비스에서 원본 컬럼 조건으로 변환해 조회한다.
     */
    @GetMapping
    public ResponseEntity<Page<InquiryAdminResponse>> getInquiries(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) InquiryStatus status,
            @RequestParam(required = false) InquiryCategory category,
            Pageable pageable
    ) {
        return ResponseEntity.ok(
                inquiryService.getInquiriesForAdmin(userId, status, category, pageable));
    }

    @GetMapping("/{inquiryId}")
    public ResponseEntity<InquiryAdminResponse> getInquiry(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long inquiryId
    ) {
        return ResponseEntity.ok(inquiryService.getInquiryForAdmin(userId, inquiryId));
    }

    /** 답변 등록과 수정을 같은 엔드포인트로 처리한다. */
    @PostMapping("/{inquiryId}/answer")
    public ResponseEntity<Void> answerInquiry(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long inquiryId,
            @Valid @RequestBody InquiryAnswerRequest request
    ) {
        inquiryService.answer(userId, inquiryId, request);
        return ResponseEntity.ok().build();
    }
}
