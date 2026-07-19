package com.signal.global.file;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class LocalFileStorageTest {

    @TempDir
    Path tempDir;

    @Test
    void 파일을_저장하면_baseUrl과_확장자를_포함한_URL을_반환한다() throws Exception {
        LocalFileStorage storage = new LocalFileStorage(tempDir.toString(), "/uploads");
        storage.init();
        MockMultipartFile image = new MockMultipartFile("image", "profile.PNG", "image/png", new byte[]{1, 2, 3});

        String url = storage.store(image, "profile");

        assertThat(url).startsWith("/uploads/profile/").endsWith(".PNG");

        String filename = url.substring(url.lastIndexOf('/') + 1);
        Path savedFile = tempDir.resolve("profile").resolve(filename);
        assertThat(Files.exists(savedFile)).isTrue();
        assertThat(Files.readAllBytes(savedFile)).containsExactly(1, 2, 3);
    }

    @Test
    void 확장자가_없는_파일도_저장된다() throws Exception {
        LocalFileStorage storage = new LocalFileStorage(tempDir.toString(), "/uploads");
        storage.init();
        MockMultipartFile image = new MockMultipartFile("image", "noext", "image/png", new byte[]{1});

        String url = storage.store(image, "profile");

        assertThat(url).matches("^/uploads/profile/[0-9a-fA-F-]{36}$");
    }
}
