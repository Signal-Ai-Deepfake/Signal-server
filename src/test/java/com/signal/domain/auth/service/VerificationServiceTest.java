package com.signal.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import com.signal.domain.auth.service.VerificationService.Purpose;
import com.signal.global.exception.ErrorCode;
import com.signal.global.exception.SignalException;
import com.signal.global.mail.MailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class VerificationServiceTest {

    private static final String EMAIL = "user@example.com";

    @Mock
    private MailService mailService;

    private VerificationService verificationService;

    @BeforeEach
    void setUp() {
        verificationService = new VerificationService(mailService);
        ReflectionTestUtils.setField(verificationService, "codeExpiration", 300_000L);
        ReflectionTestUtils.setField(verificationService, "tokenExpiration", 600_000L);
    }

    private String sendAndCaptureCode(Purpose purpose) {
        verificationService.sendCode(EMAIL, purpose);
        ArgumentCaptor<String> captor = forClass(String.class);
        verify(mailService, atLeastOnce()).sendVerificationCode(eq(EMAIL), captor.capture());
        return captor.getValue();
    }

    private void assertErrorCode(Runnable action, ErrorCode expected) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(SignalException.class, e -> assertThat(e.getErrorCode()).isEqualTo(expected));
    }

    @Test
    void 올바른_인증번호를_입력하면_verificationToken이_발급된다() {
        String code = sendAndCaptureCode(Purpose.SIGNUP);

        String token = verificationService.verifyCode(EMAIL, code, Purpose.SIGNUP);

        assertThat(token).startsWith("vrf_");
    }

    @Test
    void 인증번호를_연속으로_틀리면_올바른_번호를_입력해도_더_이상_통과하지_못한다() {
        String code = sendAndCaptureCode(Purpose.PASSWORD_RESET);
        String wrong = code.equals("000000") ? "000001" : "000000";

        for (int i = 0; i < 4; i++) {
            assertErrorCode(() -> verificationService.verifyCode(EMAIL, wrong, Purpose.PASSWORD_RESET),
                    ErrorCode.CODE_MISMATCH);
        }
        assertErrorCode(() -> verificationService.verifyCode(EMAIL, wrong, Purpose.PASSWORD_RESET),
                ErrorCode.CODE_MISMATCH);

        assertErrorCode(() -> verificationService.verifyCode(EMAIL, code, Purpose.PASSWORD_RESET),
                ErrorCode.CODE_EXPIRED);
    }

    @Test
    void 한도_이내로_틀린_뒤에는_올바른_번호로_인증할_수_있다() {
        String code = sendAndCaptureCode(Purpose.SIGNUP);
        String wrong = code.equals("000000") ? "000001" : "000000";

        assertErrorCode(() -> verificationService.verifyCode(EMAIL, wrong, Purpose.SIGNUP), ErrorCode.CODE_MISMATCH);

        assertThat(verificationService.verifyCode(EMAIL, code, Purpose.SIGNUP)).startsWith("vrf_");
    }

    @Test
    void 인증번호는_한_번만_사용할_수_있다() {
        String code = sendAndCaptureCode(Purpose.SIGNUP);
        verificationService.verifyCode(EMAIL, code, Purpose.SIGNUP);

        assertErrorCode(() -> verificationService.verifyCode(EMAIL, code, Purpose.SIGNUP), ErrorCode.CODE_EXPIRED);
    }

    @Test
    void verificationToken은_한_번만_소모할_수_있다() {
        String code = sendAndCaptureCode(Purpose.SIGNUP);
        String token = verificationService.verifyCode(EMAIL, code, Purpose.SIGNUP);

        verificationService.consumeToken(token, EMAIL, Purpose.SIGNUP);

        assertErrorCode(() -> verificationService.consumeToken(token, EMAIL, Purpose.SIGNUP),
                ErrorCode.INVALID_VERIFICATION);
    }

    @Test
    void verificationToken은_발급받은_이메일과_용도에서만_사용할_수_있다() {
        String code = sendAndCaptureCode(Purpose.SIGNUP);
        String token = verificationService.verifyCode(EMAIL, code, Purpose.SIGNUP);

        assertErrorCode(() -> verificationService.consumeToken(token, "other@example.com", Purpose.SIGNUP),
                ErrorCode.INVALID_VERIFICATION);
        assertErrorCode(() -> verificationService.consumeToken(token, EMAIL, Purpose.PASSWORD_RESET),
                ErrorCode.INVALID_VERIFICATION);
        verificationService.consumeToken(token, EMAIL, Purpose.SIGNUP);
    }
}
