package com.lawchat.global.file;

import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * DB에는 파일명만 저장하지만, 응답에는 브라우저가 바로 쓸 수 있는 절대 URL 을 내려준다.
 *
 * 이렇게 하는 이유:
 *  - 프론트가 "http://.../api/files/view/" + encodeURIComponent(파일명) 을 매번 조합할 필요가 없다.
 *  - 한글 파일명 인코딩 실수를 원천 차단한다.
 *  - 나중에 공유폴더 서버가 바뀌거나 CDN 을 붙여도 이 클래스만 고치면 된다.
 *
 * 주의: 리버스 프록시(nginx 등) 뒤에 배포하면 프록시가 X-Forwarded-* 헤더를 넘겨줘야
 *      외부 도메인 기준 URL 이 만들어진다. (application.yml 에
 *      server.forward-headers-strategy: framework 설정 필요)
 */
public final class FileUrls {

    private static final String VIEW_PREFIX = "/api/files/view/";
    private static final String DOWNLOAD_PREFIX = "/api/files/download/";

    private FileUrls() {
    }

    /** 이미지 등 브라우저에서 바로 렌더링할 파일 (팝업 이미지용) */
    public static String view(String filename) {
        return build(VIEW_PREFIX, filename);
    }

    /** 다른 이름으로 저장되는 첨부파일 (공지 첨부용) */
    public static String download(String filename) {
        return build(DOWNLOAD_PREFIX, filename);
    }

    private static String build(String prefix, String filename) {
        if (filename == null || filename.isBlank()) {
            return null;
        }
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(prefix)
                .path(encoded)
                .toUriString();
    }
}
