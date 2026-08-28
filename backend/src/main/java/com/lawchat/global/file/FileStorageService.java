package com.lawchat.global.file;

import com.lawchat.global.exception.BusinessException;
import com.lawchat.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * 공유폴더(Z드라이브 매핑 대상 서버)를 UNC 경로로 직접 접근하는 파일 저장/조회 서비스.
 *
 * 설계 원칙:
 * - 앱 시작 시점(@PostConstruct 등)에 연결 여부를 검증하지 않는다.
 *   -> 공유폴더가 잠깐 끊겨 있어도 스프링부트 앱 자체는 정상 기동해야 하기 때문.
 * - 연결 확인/실패 처리는 오직 파일 접근이 실제로 호출되는 시점에만 한다.
 *   -> 실패해도 해당 요청 하나만 실패시키고 서버 전체에는 영향 없음.
 */
@Slf4j
@Service
public class FileStorageService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    /**
     * @return 저장된 파일명. 이 값을 notices.file_url / notice_popups.file_url 에 저장한다.
     *         (UNC 경로 전체를 저장하지 않는 이유: 서버 위치가 바뀌면 전체 마이그레이션이 필요해짐)
     */
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_FILE);
        }

        String storedFilename = UUID.randomUUID() + "_" + sanitize(file.getOriginalFilename());
        Path targetPath = Paths.get(uploadDir).resolve(storedFilename);

        try {
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            // 원인(네트워크 경로 끊김, 권한 문제 등)은 로그에만 남기고
            // 클라이언트에는 일반화된 메시지만 노출한다.
            log.warn("파일 업로드 실패 - uploadDir={}, filename={}, cause={}",
                    uploadDir, storedFilename, e.getMessage());
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        }

        return storedFilename;
    }

    /**
     * 공유폴더는 웹에서 직접 접근할 수 없으므로 서버를 경유해 파일을 읽어 내려준다.
     * 팝업 이미지(&lt;img src&gt;)와 공지 첨부 다운로드 모두 이 경로를 사용.
     */
    public Resource loadAsResource(String filename) {
        Path filePath = resolveSafely(filename);

        try {
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
            }
            return resource;
        } catch (MalformedURLException e) {
            log.warn("파일 경로 변환 실패 - filename={}, cause={}", filename, e.getMessage());
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
        }
    }

    public String probeContentType(Resource resource) {
        try {
            String contentType = Files.probeContentType(Paths.get(resource.getURI()));
            return (contentType != null) ? contentType : "application/octet-stream";
        } catch (IOException e) {
            return "application/octet-stream";
        }
    }

    /**
     * 경로 조작(../../ 로 공유폴더 밖 파일 읽기) 차단.
     * 정규화한 결과가 uploadDir 하위가 아니면 거부한다.
     */
    private Path resolveSafely(String filename) {
        Path baseDir = Paths.get(uploadDir).normalize();
        Path resolved = baseDir.resolve(filename).normalize();

        if (!resolved.startsWith(baseDir)) {
            log.warn("허용되지 않은 파일 경로 접근 시도 - filename={}", filename);
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
        }
        return resolved;
    }

    private String sanitize(String originalFilename) {
        if (originalFilename == null) {
            return "file";
        }
        return originalFilename.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}
