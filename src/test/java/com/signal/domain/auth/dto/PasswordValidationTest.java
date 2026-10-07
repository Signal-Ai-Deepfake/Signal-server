package com.signal.domain.auth.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.signal.domain.auth.dto.request.ResetPasswordRequest;
import com.signal.domain.auth.dto.request.SignupRequest;
import com.signal.domain.user.entity.User;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

class PasswordValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private SignupRequest signup(String password) {
        return new SignupRequest("a@b.com", password, "vrf_x", "홍길동", 20, User.Gender.MALE,
                new SignupRequest.Agreements(true, true));
    }

    private ResetPasswordRequest reset(String password) {
        return new ResetPasswordRequest("a@b.com", "vrf_x", password, password);
    }

    @Test
    void 비밀번호가_8자_미만이면_가입과_재설정_모두_거부한다() {
        assertThat(validator.validate(signup("short12"))).hasSize(1);
        assertThat(validator.validate(reset("short12"))).hasSize(1);
    }

    @Test
    void 비밀번호가_bcrypt_한도인_72자를_넘으면_거부한다() {
        assertThat(validator.validate(signup("a".repeat(73)))).hasSize(1);
        assertThat(validator.validate(reset("a".repeat(73)))).hasSize(1);
    }

    @Test
    void 비밀번호가_8자_이상_72자_이하이면_통과한다() {
        assertThat(validator.validate(signup("password1"))).isEmpty();
        assertThat(validator.validate(reset("a".repeat(72)))).isEmpty();
    }
}
