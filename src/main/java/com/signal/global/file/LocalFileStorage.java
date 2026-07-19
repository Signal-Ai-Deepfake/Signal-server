package com.signal.global.file;

import com.signal.global.exception.ErrorCode;
import com.signal.global.exception.SignalException;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Component
public class LocalFileStorage implements FileStorage {

    private final Path rootDir;
    private final String baseUrl;

    public LocalFileStorage(@Value("${file.upload-dir}") String uploadDir,
                             @Value("${file.base-url}") String baseUrl) {
        this.rootDir = Path.of(uploadDir).toAbsolutePath().normalize();
        this.baseUrl = baseUrl;
    }

    @PostConstruct
    void init() {
        try {
            Files.createDirectories(rootDir);
        } catch (IOException e) {
            log.error("업로드 디렉토리 생성 실패: {}", rootDir, e);
            throw new IllegalStateException("업로드 디렉토리를 생성할 수 없습니다.", e);
        }
    }

    @Override
    public String store(MultipartFile file, String directory) {
        String filename = UUID.randomUUID() + extractExtension(file.getOriginalFilename());
        Path targetDir = rootDir.resolve(directory).normalize();

        try {
            Files.createDirectories(targetDir);
            Path target = targetDir.resolve(filename);
            file.transferTo(target);
        } catch (IOException e) {
            log.error("파일 저장 실패: {}", filename, e);
            throw new SignalException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        return baseUrl + "/" + directory + "/" + filename;
    }

    private String extractExtension(String originalFilename) {
        if (originalFilename == null) {
            return "";
        }
        int dotIndex = originalFilename.lastIndexOf('.');
        return dotIndex >= 0 ? originalFilename.substring(dotIndex) : "";
    }
}
