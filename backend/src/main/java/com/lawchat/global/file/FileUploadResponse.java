package com.lawchat.global.file;

/**
 * 파일 업로드 공용 응답.
 *
 * 기존에는 컨트롤러마다 Map.of("fileName", ..., "fileUrl", ...) 를 직접 만들었다.
 * 키를 오타 내도 컴파일이 통과하고 Swagger 문서에도 형태가 드러나지 않아 record 로 고정한다.
 *
 * JSON 형태는 기존 Map 과 완전히 동일하므로 프론트 수정은 필요 없다.
 *   fileName - 등록 요청(screenshotUrl / fileUrl 필드)에 그대로 넣을 값 (DB 저장용)
 *   fileUrl  - 업로드 직후 미리보기에 쓸 절대 URL (&lt;img src&gt;)
 */
public record FileUploadResponse(String fileName, String fileUrl) {

    public static FileUploadResponse of(String storedFilename) {
        return new FileUploadResponse(storedFilename, FileUrls.view(storedFilename));
    }
}
