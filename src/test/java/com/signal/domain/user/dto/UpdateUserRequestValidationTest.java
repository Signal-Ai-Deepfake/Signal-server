package com.signal.domain.user.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.signal.domain.user.dto.request.UpdateUserRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

class UpdateUserRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void 변경하지_않는_필드는_null이어도_통과한다() {
        assertThat(validator.validate(new UpdateUserRequest(null, null, null))).isEmpty();
    }

    @Test
    void 이름이_공백이거나_50자를_넘으면_거부한다() {
        assertThat(validator.validate(new UpdateUserRequest("", null, null))).isNotEmpty();
        assertThat(validator.validate(new UpdateUserRequest("   ", null, null))).isNotEmpty();
        assertThat(validator.validate(new UpdateUserRequest("가".repeat(51), null, null))).isNotEmpty();
        assertThat(validator.validate(new UpdateUserRequest("가".repeat(50), null, null))).isEmpty();
    }

    @Test
    void 나이가_범위를_벗어나면_거부한다() {
        assertThat(validator.validate(new UpdateUserRequest(null, 0, null))).isNotEmpty();
        assertThat(validator.validate(new UpdateUserRequest(null, -3, null))).isNotEmpty();
        assertThat(validator.validate(new UpdateUserRequest(null, 151, null))).isNotEmpty();
        assertThat(validator.validate(new UpdateUserRequest(null, 25, null))).isEmpty();
    }
}
