package com.lawchat.global.file;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.Duration;

/**
 * 공유폴더(UNC 경로)에 저장된 파일을 서버를 경유해 내려주는 API.
 * 로그인 없이도 접근 가능 — 공지 첨부와 팝업 이미지는 비로그인 사용자에게도 보여야 하기 때문.
 *
 * ★ 경로 매핑에 정규식을 붙인 이유
 *   기본 매핑 "/view/{filename}" 은 파일명에 점(.)이 있어도 Spring Boot 3 에서는 잘리지 않지만,
 *   {filename:.+} 로 명시해 두면 어떤 설정에서도 확장자가 안전하게 유지된다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/files")
public class FileController {

    private final FileStorageService fileStorageService;

    /** 팝업 이미지·문의 스크린샷 등 브라우저에서 바로 렌더링 (&lt;img src="/api/files/view/{filename}"&gt;) */
    @GetMapping("/view/{filename:.+}")
    public ResponseEntity<Resource> view(@PathVariable String filename) {
        String normalized = normalize(filename);
        Resource resource = fileStorageService.loadAsResource(normalized);
        String contentType = fileStorageService.probeContentType(normalized);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                // UUID 가 붙은 파일명이라 내용이 바뀔 일이 없다. 브라우저 캐시를 허용해 재요청을 줄인다.
                .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePublic())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(resource);
    }

    /** 공지 첨부파일 다운로드 (다른 이름으로 저장) */
    @GetMapping("/download/{filename:.+}")
    public ResponseEntity<Resource> download(@PathVariable String filename) {
        String normalized = normalize(filename);
        Resource resource = fileStorageService.loadAsResource(normalized);

        // RFC 5987 형식. 한글 파일명을 그대로 filename= 에 넣으면 브라우저가 깨뜨린다.
        String encoded = URLEncoder.encode(normalized, StandardCharsets.UTF_8).replace("+", "%20");

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + encoded)
                .body(resource);
    }

    /**
     * 저장 시점과 동일하게 NFC 로 맞춘다.
     * 브라우저·OS 에 따라 한글이 NFD(자모 분리) 형태로 요청될 수 있어,
     * 정규화하지 않으면 눈에는 같은 이름인데 파일을 못 찾는다.
     */
    private String normalize(String filename) {
        return Normalizer.normalize(filename, Normalizer.Form.NFC);
    }
}
