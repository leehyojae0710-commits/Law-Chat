package com.lawchat.domain.notice.controller;

import com.lawchat.domain.notice.dto.response.NoticeDetailResponse;
import com.lawchat.domain.notice.dto.response.NoticeListResponse;
import com.lawchat.domain.notice.dto.response.NoticePopupResponse;
import com.lawchat.domain.notice.entity.NoticeCategory;
import com.lawchat.domain.notice.service.NoticePopupService;
import com.lawchat.domain.notice.service.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 로그인 여부와 무관하게 접근 가능한 조회 전용 API.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notices")
public class NoticeController {

    private final NoticeService noticeService;
    private final NoticePopupService noticePopupService;

    /** 정렬은 서버에서 고정(고정공지 우선 → 최신순). page, size 만 유효. */
    @GetMapping
    public ResponseEntity<Page<NoticeListResponse>> getNotices(
            @RequestParam(required = false) NoticeCategory category,
            Pageable pageable
    ) {
        return ResponseEntity.ok(noticeService.getNotices(category, pageable));
    }

    @GetMapping("/{noticeId}")
    public ResponseEntity<NoticeDetailResponse> getNotice(@PathVariable Long noticeId) {
        return ResponseEntity.ok(noticeService.getNotice(noticeId));
    }

    @GetMapping("/popups/active")
    public ResponseEntity<List<NoticePopupResponse>> getActivePopups() {
        return ResponseEntity.ok(noticePopupService.getActivePopups());
    }
}
