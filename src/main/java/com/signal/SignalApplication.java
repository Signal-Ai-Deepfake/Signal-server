package com.signal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync // 메일 발송 비동기 처리용
public class SignalApplication {

    public static void main(String[] args) {
        SpringApplication.run(SignalApplication.class, args);
    }
}
