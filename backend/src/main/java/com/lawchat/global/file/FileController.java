package com.lawchat.global.file;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 공유폴더(UNC 경로)에 저장된 파일을 서버를 경유해 내려주는 API.
 * 로그인 없이도 접근 가능 — 공지 첨부와 팝업 이미지는 비로그인 사용자에게도 보여야 하기 때문.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/files")
public class FileController {

    private final FileStorageService fileStorageService;

    /** 팝업 이미지 등 브라우저에서 바로 렌더링 (&lt;img src="/api/files/view/{filename}"&gt;) */
    @GetMapping("/view/{filename}")
    public ResponseEntity<Resource> view(@PathVariable String filename) {
        Resource resource = fileStorageService.loadAsResource(filename);
        String contentType = fileStorageService.probeContentType(resource);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }

    /** 공지 첨부파일 다운로드 (다른 이름으로 저장) */
    @GetMapping("/download/{filename}")
    public ResponseEntity<Resource> download(@PathVariable String filename) {
        Resource resource = fileStorageService.loadAsResource(filename);
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + encoded)
                .body(resource);
    }
}
