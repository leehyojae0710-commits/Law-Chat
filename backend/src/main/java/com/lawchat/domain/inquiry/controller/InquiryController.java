package com.lawchat.domain.inquiry.controller;

import com.lawchat.domain.inquiry.dto.request.InquiryCreateRequest;
import com.lawchat.domain.inquiry.dto.response.InquiryDetailResponse;
import com.lawchat.domain.inquiry.dto.response.InquirySummaryResponse;
import com.lawchat.domain.inquiry.service.InquiryService;
import com.lawchat.global.file.FileStorageService;
import com.lawchat.global.file.FileUrls;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 사용자용 1:1 문의 API.
 *
 * SecurityConfig 의 anyRequest().authenticated() 에 걸리므로 별도 설정 없이 로그인 필수다.
 * (공지사항처럼 permitAll 목록에 추가하면 안 된다 — 본인 문의만 조회해야 하므로)
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/inquiries")
public class InquiryController {

    private final InquiryService inquiryService;
    private final FileStorageService fileStorageService;

    @PostMapping
    public ResponseEntity<Long> createInquiry(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody InquiryCreateRequest request
    ) {
        return ResponseEntity.ok(inquiryService.create(userId, request));
    }

    /** 정렬은 서버에서 최신순으로 고정. page, size 만 유효. */
    @GetMapping("/me")
    public ResponseEntity<Page<InquirySummaryResponse>> getMyInquiries(
            @AuthenticationPrincipal Long userId,
            Pageable pageable
    ) {
        return ResponseEntity.ok(inquiryService.getMyInquiries(userId, pageable));
    }

    @GetMapping("/{inquiryId}")
    public ResponseEntity<InquiryDetailResponse> getMyInquiry(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long inquiryId
    ) {
        return ResponseEntity.ok(inquiryService.getMyInquiry(userId, inquiryId));
    }

    /** 답변 등록 전에만 가능. 이미 답변된 문의면 INQUIRY_ALREADY_ANSWERED (409). */
    @DeleteMapping("/{inquiryId}")
    public ResponseEntity<Void> deleteInquiry(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long inquiryId
    ) {
        inquiryService.delete(userId, inquiryId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 스크린샷 업로드. 공지 첨부 업로드(/api/admin/notices/upload)와 같은 규칙을 따른다.
     *
     * 응답:
     *   fileName - 문의 등록 요청의 screenshotUrl 필드에 그대로 넣을 값 (DB 저장용)
     *   fileUrl  - 업로드 직후 미리보기에 쓸 절대 URL (&lt;img src&gt;)
     *
     * 관리자 업로드와 달리 AdminValidator 를 호출하지 않는다. 일반 사용자가 써야 하기 때문.
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadScreenshot(
            @AuthenticationPrincipal Long userId,
            @RequestParam("file") MultipartFile file
    ) {
        String storedFilename = fileStorageService.store(file);
        return ResponseEntity.ok(Map.of(
                "fileName", storedFilename,
                "fileUrl", FileUrls.view(storedFilename)
        ));
    }
}
