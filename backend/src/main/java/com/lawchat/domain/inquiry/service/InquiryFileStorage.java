package com.lawchat.domain.inquiry.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * 문의 스크린샷 저장. screenshot_url 이 varchar(512) 라 URL 문자열만 DB에 넣습니다.
 * S3 로 바꾸더라도 이 클래스만 교체하면 됩니다.
 */
@Service
public class InquiryFileStorage {

    private static final List<String> ALLOWED_TYPES =
            List.of("image/png", "image/jpeg", "image/gif", "image/webp");
    private static final long MAX_BYTES = 5 * 1024 * 1024;

    private final Path uploadRoot;
    private final String publicPrefix;

    public InquiryFileStorage(
            @Value("${app.upload.inquiry-dir:./uploads/inquiries}") String uploadDir,
            @Value("${app.upload.public-prefix:/files/inquiries}") String publicPrefix) {
        this.uploadRoot = Path.of(uploadDir);
        this.publicPrefix = publicPrefix;
    }

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "첨부할 이미지를 선택해주세요.");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "첨부 이미지는 5MB까지 올릴 수 있습니다.");
        }
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "PNG, JPG, GIF, WEBP 이미지만 첨부할 수 있습니다.");
        }

        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        String extension = resolveExtension(file.getContentType());
        String filename = UUID.randomUUID() + extension;

        try {
            Path directory = uploadRoot.resolve(datePath);
            Files.createDirectories(directory);
            Path target = directory.resolve(filename);
            try (var in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "이미지를 저장하지 못했습니다.", e);
        }

        // 원본 파일명을 쓰지 않아 경로 조작과 한글 파일명 문제를 함께 피합니다.
        return "%s/%s/%s".formatted(publicPrefix, datePath, filename);
    }

    private String resolveExtension(String contentType) {
        return switch (contentType) {
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
    }
}
