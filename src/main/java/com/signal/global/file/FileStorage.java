package com.signal.global.file;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorage {

    /**
     * 파일을 저장하고 접근 가능한 URL을 반환한다.
     */
    String store(MultipartFile file, String directory);
}
