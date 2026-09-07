package com.lawchat.global.file;

import com.lawchat.global.exception.BusinessException;
import com.lawchat.global.exception.ErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 이미지 업로드 공용 검증기.
 *
 * ★ ProfileImageValidator 를 여기로 옮긴 이유
 *   프로필 이미지 / 문의 스크린샷 / 공지·팝업 이미지가 모두 같은 규칙(이미지 5MB)을 쓰는데
 *   검증이 프로필에만 걸려 있었다. 문의·공지 업로드는 아무 파일이나 통과하던 상태였다.
 *   같은 규칙을 세 곳에 복붙하면 다시 어긋나므로 global.file 로 올려 하나만 두었다.
 *
 * ★ FileStorageService 와 분리해 둔 이유
 *   FileStorageService 는 "저장"만 책임진다. "이 파일이 올라와도 되는가"는 다른 관심사라
 *   호출하는 컨트롤러가 정책을 골라 걸도록 남겨 둔다.
 *   나중에 공지 첨부로 PDF·문서를 받아야 하면 이 클래스에 validateAttachment() 를 추가하고
 *   해당 컨트롤러만 그쪽을 호출하면 된다. (지금은 프론트가 전부 accept="image/*")
 */
@Component
public class ImageUploadValidator {

    /**
     * 허용 형식. SVG 는 의도적으로 제외한다.
     * SVG 는 내부에 스크립트를 품을 수 있어, 그대로 렌더링하면 XSS 위험이 있다.
     */
    private static final List<String> ALLOWED_CONTENT_TYPES =
            List.of("image/png", "image/jpeg", "image/gif", "image/webp");

    /** 공유폴더 용량과 전송 시간을 고려해 5MB 로 제한. */
    private static final long MAX_BYTES = 5L * 1024 * 1024;

    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_FILE);
        }

        if (file.getSize() > MAX_BYTES) {
            throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
        }

        // getContentType() 은 클라이언트가 보낸 값이라 위조가 가능하다.
        // 다만 저장 후 <img> 로만 서빙하고 실행하지 않으므로, 이 수준의 검사로 충분하다.
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_FILE_TYPE);
        }
    }
}
