package com.signal.global.file;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.signal.global.exception.ErrorCode;
import com.signal.global.exception.SignalException;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class UploadFileValidatorTest {

    private static final long MAX_SIZE = 10L;
    private static final Set<String> ALLOWED = Set.of("image/jpeg", "image/png");

    @Test
    void 허용된_파일은_통과한다() {
        MockMultipartFile file = new MockMultipartFile("image", "a.png", "image/png", new byte[]{1, 2, 3});

        assertThatCode(() -> UploadFileValidator.validate(file, MAX_SIZE, ALLOWED)).doesNotThrowAnyException();
    }

    @Test
    void 파일이_null이면_INVALID_INPUT_예외가_발생한다() {
        assertThatThrownBy(() -> UploadFileValidator.validate(null, MAX_SIZE, ALLOWED))
                .isInstanceOf(SignalException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    @Test
    void 파일이_비어있으면_INVALID_INPUT_예외가_발생한다() {
        MockMultipartFile file = new MockMultipartFile("image", "a.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> UploadFileValidator.validate(file, MAX_SIZE, ALLOWED))
                .isInstanceOf(SignalException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }

    @Test
    void 최대_크기를_초과하면_FILE_TOO_LARGE_예외가_발생한다() {
        MockMultipartFile file = new MockMultipartFile("image", "a.png", "image/png", new byte[11]);

        assertThatThrownBy(() -> UploadFileValidator.validate(file, MAX_SIZE, ALLOWED))
                .isInstanceOf(SignalException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FILE_TOO_LARGE);
    }

    @Test
    void 허용되지_않은_content_type이면_UNSUPPORTED_MEDIA_TYPE_예외가_발생한다() {
        MockMultipartFile file = new MockMultipartFile("file", "a.gif", "image/gif", new byte[]{1});

        assertThatThrownBy(() -> UploadFileValidator.validate(file, MAX_SIZE, ALLOWED))
                .isInstanceOf(SignalException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNSUPPORTED_MEDIA_TYPE);
    }

    @Test
    void content_type이_null이면_UNSUPPORTED_MEDIA_TYPE_예외가_발생한다() {
        MockMultipartFile file = new MockMultipartFile("file", "a", null, new byte[]{1});

        assertThatThrownBy(() -> UploadFileValidator.validate(file, MAX_SIZE, ALLOWED))
                .isInstanceOf(SignalException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNSUPPORTED_MEDIA_TYPE);
    }
}
