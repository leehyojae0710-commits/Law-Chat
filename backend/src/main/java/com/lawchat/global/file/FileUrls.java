package com.lawchat.global.file;

import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

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

    /** 이미지 등 브라우저에서 바로 렌더링할 파일 (팝업 이미지, 문의 스크린샷용) */
    public static String view(String filename) {
        return build(VIEW_PREFIX, filename);
    }

    /** 다른 이름으로 저장되는 첨부파일 (공지 첨부용) */
    public static String download(String filename) {
        return build(DOWNLOAD_PREFIX, filename);
    }

    /**
     * ★ 이중 인코딩 주의
     *
     * 예전 코드는 URLEncoder.encode() 로 직접 인코딩한 문자열을 path() 에 넣었다.
     * 그런데 UriComponentsBuilder.toUriString() 은 내부적으로 build().encode().toUriString()
     * 을 수행하므로, 이미 인코딩된 값이 한 번 더 인코딩되어 % 가 %25 로 바뀌었다.
     *
     *   무당벌레.jpg
     *     -> %EB%AC%B4...      (URLEncoder)
     *     -> %25EB%25AC%25B4.. (toUriString 이 % 를 %25 로 재인코딩)
     *
     * 이 URL 로 요청하면 서버는 파일명을 "%EB%AC%B4..." 라는 리터럴 문자열로 받아
     * 공유폴더에서 찾지 못하고 404(FILE_NOT_FOUND) 를 낸다.
     *
     * 해결: 직접 인코딩하지 않고 URI 템플릿 변수로 넘긴 뒤 encode() 를 한 번만 호출한다.
     * buildAndExpand() 를 쓰면 파일명에 {, } 같은 문자가 있어도 템플릿으로 오해하지 않는다.
     */
    private static String build(String prefix, String filename) {
        if (filename == null || filename.isBlank()) {
            return null;
        }
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(prefix)
                .path("{filename}")
                .buildAndExpand(filename)
                .encode(StandardCharsets.UTF_8)
                .toUriString();
    }
}
