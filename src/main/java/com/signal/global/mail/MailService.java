package com.signal.global.mail;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    @Async
    public void sendVerificationCode(String to, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("[Signal] 인증번호 안내");
        message.setText("""
                Signal 인증번호입니다.

                인증번호: %s

                5분 안에 입력해 주세요.
                본인이 요청하지 않았다면 이 메일을 무시하세요.
                """.formatted(code));

        mailSender.send(message);
        log.info("인증번호 메일 발송 완료: {}", to);
    }
}
