package com.lawchat.domain.notice.controller;

import com.lawchat.domain.notice.dto.request.NoticeCreateRequest;
import com.lawchat.domain.notice.dto.request.NoticePopupCreateRequest;
import com.lawchat.domain.notice.dto.request.NoticePopupUpdateRequest;
import com.lawchat.domain.notice.dto.request.NoticeUpdateRequest;
import com.lawchat.domain.notice.dto.response.NoticePopupAdminResponse;
import com.lawchat.domain.notice.service.NoticePopupService;
import com.lawchat.domain.notice.service.NoticeService;
import com.lawchat.global.auth.AdminValidator;
import com.lawchat.global.file.FileStorageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 관리자 전용 API. 권한 검증은 각 서비스 메서드 내부에서 User.isActiveAdmin() 으로 수행.
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

    @PostMapping
    public ResponseEntity<Long> createNotice(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody NoticeCreateRequest request
    ) {
        return ResponseEntity.ok(noticeService.create(userId, request));
    }

    @PatchMapping("/{noticeId}")
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

    /** 노출 기간 연장 등 부분 수정 */
    @PatchMapping("/popups/{popupId}")
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
     * 공유폴더(UNC 경로)로 파일 업로드. 성공 시 저장된 파일명을 반환하며,
     * 이 값을 NoticeCreateRequest.fileUrl / NoticePopupCreateRequest.fileUrl 에 넣어 사용.
     * 실제 조회는 /api/files/view/{filename} 또는 /api/files/download/{filename}.
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadFile(
            @AuthenticationPrincipal Long userId,
            @RequestParam("file") MultipartFile file
    ) {
        adminValidator.validate(userId);

        String storedFilename = fileStorageService.store(file);
        return ResponseEntity.ok(Map.of("fileUrl", storedFilename));
    }
}
