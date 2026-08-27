package com.lawchat.domain.notice.controller;

import com.lawchat.domain.notice.dto.request.NoticeCreateRequest;
import com.lawchat.domain.notice.dto.request.NoticePopupCreateRequest;
import com.lawchat.domain.notice.dto.request.NoticePopupUpdateRequest;
import com.lawchat.domain.notice.dto.request.NoticeUpdateRequest;
import com.lawchat.domain.notice.dto.response.NoticeListResponse;
import com.lawchat.domain.notice.dto.response.NoticePopupAdminResponse;
import com.lawchat.domain.notice.entity.NoticeCategory;
import com.lawchat.domain.notice.service.NoticePopupService;
import com.lawchat.domain.notice.service.NoticeService;
import com.lawchat.global.auth.AdminValidator;
import com.lawchat.global.file.FileStorageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 관리자 전용 API. 권한 검증은 각 서비스 메서드 내부에서 AdminValidator 로 수행.
 * (인증 principal 이 단순 Long userId 라 @PreAuthorize 대신 서비스 레벨 체크 채택)
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/notices")
public class AdminNoticeController {

    private final NoticeService noticeService;
    private final NoticePopupService noticePopupService;
    private final FileStorageService fileStorageService;
    private final AdminValidator adminValidator;

    // ---------- 공지사항 ----------

    /**
     * 관리자 공지 목록. 응답 형태는 공개 목록(GET /api/notices)과 동일한 Page 구조라
     * 프론트에서 목록 렌더링 로직을 공용으로 쓸 수 있다.
     */
    @GetMapping
    public ResponseEntity<Page<NoticeListResponse>> getNotices(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) NoticeCategory category,
            Pageable pageable
    ) {
        return ResponseEntity.ok(noticeService.getNoticesForAdmin(userId, category, pageable));
    }

    @PostMapping
    public ResponseEntity<Long> createNotice(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody NoticeCreateRequest request
    ) {
        return ResponseEntity.ok(noticeService.create(userId, request));
    }

    /**
     * 부분 수정. PUT / PATCH 둘 다 받는다.
     * (의미상으로는 PATCH 가 맞지만, 프론트에서 PUT 으로 부르고 있어 405 를 피하려고 함께 매핑)
     * 보내지 않거나 null 인 필드는 기존 값이 유지된다.
     */
    @RequestMapping(value = "/{noticeId}", method = {RequestMethod.PATCH, RequestMethod.PUT})
    public ResponseEntity<Void> updateNotice(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long noticeId,
            @RequestBody NoticeUpdateRequest request
    ) {
        noticeService.update(userId, noticeId, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{noticeId}")
    public ResponseEntity<Void> deleteNotice(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long noticeId
    ) {
        noticeService.delete(userId, noticeId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{noticeId}/pin")
    public ResponseEntity<Void> togglePin(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long noticeId
    ) {
        noticeService.togglePin(userId, noticeId);
        return ResponseEntity.ok().build();
    }

    // ---------- 팝업 ----------

    /** 기간 지난 팝업까지 전체 조회 (관리 화면에서 확인/연장/삭제 판단용) */
    @GetMapping("/popups")
    public ResponseEntity<List<NoticePopupAdminResponse>> getAllPopups(
            @AuthenticationPrincipal Long userId
    ) {
        return ResponseEntity.ok(noticePopupService.getAllPopups(userId));
    }

    @PostMapping("/popups")
    public ResponseEntity<Long> createPopup(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody NoticePopupCreateRequest request
    ) {
        return ResponseEntity.ok(noticePopupService.create(userId, request));
    }

    /** 노출 기간 연장 등 부분 수정. PUT / PATCH 둘 다 허용. */
    @RequestMapping(value = "/popups/{popupId}", method = {RequestMethod.PATCH, RequestMethod.PUT})
    public ResponseEntity<Void> updatePopup(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long popupId,
            @RequestBody NoticePopupUpdateRequest request
    ) {
        noticePopupService.update(userId, popupId, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/popups/{popupId}")
    public ResponseEntity<Void> deletePopup(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long popupId
    ) {
        noticePopupService.delete(userId, popupId);
        return ResponseEntity.noContent().build();
    }

    // ---------- 파일 업로드 ----------

    /**
     * 공유폴더(UNC 경로)로 파일 업로드.
     *
     * 응답:
     *   fileName - 공지/팝업 등록 요청의 fileUrl 필드에 그대로 넣을 값 (DB 저장용)
     *   fileUrl  - 업로드 직후 미리보기에 쓸 절대 URL (<img src>)
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadFile(
            @AuthenticationPrincipal Long userId,
            @RequestParam("file") MultipartFile file
    ) {
        adminValidator.validate(userId);

        String storedFilename = fileStorageService.store(file);
        return ResponseEntity.ok(Map.of(
                "fileName", storedFilename,
                "fileUrl", com.lawchat.global.file.FileUrls.view(storedFilename)
        ));
    }
}
