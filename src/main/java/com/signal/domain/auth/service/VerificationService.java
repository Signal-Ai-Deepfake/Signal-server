package com.signal.domain.auth.service;

import com.signal.global.exception.ErrorCode;
import com.signal.global.exception.SignalException;
import com.signal.global.mail.MailService;
import java.security.SecureRandom;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 인증번호 발송 → 확인 → verificationToken 발급까지 담당.
 * 저장소는 인메모리(ConcurrentHashMap). 서버 여러 대로 확장하면 Redis로 교체할 것.
 */
@Service
@RequiredArgsConstructor
public class VerificationService {

    public enum Purpose { SIGNUP, PASSWORD_RESET }

    private record CodeEntry(String code, long expiresAt) {}

    private record TokenEntry(String email, Purpose purpose, long expiresAt) {}

    private static final SecureRandom RANDOM = new SecureRandom();

    private final MailService mailService;

    @Value("${verification.code-expiration}")
    private long codeExpiration;

    @Value("${verification.token-expiration}")
    private long tokenExpiration;

    private final Map<String, CodeEntry> codes = new ConcurrentHashMap<>();
    private final Map<String, TokenEntry> tokens = new ConcurrentHashMap<>();

    /** 인증번호 생성·저장 후 메일 발송. 유효시간(초) 반환 */
    public long sendCode(String email, Purpose purpose) {
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        codes.put(key(email, purpose), new CodeEntry(code, now() + codeExpiration));
        mailService.sendVerificationCode(email, code);
        return codeExpiration / 1000;
    }

    /** 인증번호 확인 성공 시 verificationToken 발급 */
    public String verifyCode(String email, String code, Purpose purpose) {
        CodeEntry entry = codes.get(key(email, purpose));

        if (entry == null || now() > entry.expiresAt()) {
            throw new SignalException(ErrorCode.CODE_EXPIRED);
        }
        if (!entry.code().equals(code)) {
            throw new SignalException(ErrorCode.CODE_MISMATCH);
        }

        codes.remove(key(email, purpose));

        String token = "vrf_" + UUID.randomUUID().toString().replace("-", "");
        tokens.put(token, new TokenEntry(email, purpose, now() + tokenExpiration));
        return token;
    }

    /** verificationToken 검증 후 소모(1회용) */
    public void consumeToken(String token, String email, Purpose purpose) {
        TokenEntry entry = tokens.get(token);

        if (entry == null
                || now() > entry.expiresAt()
                || !entry.email().equals(email)
                || entry.purpose() != purpose) {
            throw new SignalException(ErrorCode.INVALID_VERIFICATION);
        }

        tokens.remove(token);
    }

    private String key(String email, Purpose purpose) {
        return purpose + ":" + email;
    }

    private long now() {
        return System.currentTimeMillis();
    }
}
