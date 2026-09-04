package com.lawchat.domain.user.service;

import com.lawchat.global.exception.BusinessException;
import com.lawchat.global.exception.ErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 프로필 이미지 업로드 검증.
 *
 * ★ 왜 UserService 안에 두지 않고 별도 클래스로 뺐는가
 *   UserService 는 이미 회원가입/로그인/소셜연동/탈퇴까지 담당해 충분히 크다.
 *   "이 파일이 프로필 사진으로 적합한가"는 성격이 다른 관심사이므로 분리했다.
 *   나중에 썸네일 리사이즈 같은 요구가 생겨도 이 클래스만 손대면 된다.
 *
 * ★ 왜 FileStorageService 에 넣지 않았는가
 *   FileStorageService 는 공지 첨부(PDF·문서 등)도 처리하는 범용 저장소다.
 *   거기에 "이미지만 허용" 규칙을 넣으면 공지 첨부가 깨진다.
 *   용도별 제약은 호출하는 쪽에서 거는 것이 맞다.
 */
@Component
public class ProfileImageValidator {

    /**
     * 허용 형식. SVG 는 의도적으로 제외한다.
     * SVG 는 내부에 스크립트를 품을 수 있어, 그대로 렌더링하면 XSS 위험이 있다.
     * (문의 스크린샷 업로드도 동일한 이유로 SVG 를 빼고 있다)
     */
    private static final List<String> ALLOWED_CONTENT_TYPES =
            List.of("image/png", "image/jpeg", "image/gif", "image/webp");

    /** 프로필 사진은 크게 쓸 일이 없다. 공유폴더 용량과 전송 시간을 고려해 5MB 로 제한. */
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
        // (실행 파일을 이미지로 위장해도 브라우저가 이미지로 해석하지 못할 뿐이다)
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_FILE_TYPE);
        }
    }
}
