package com.signal.domain.chat.engine;

/**
 * 대화 기록(history)에서 한 턴의 발화자. 영속성 계층의 {@code ChatRole}과 별개로 두어
 * engine 패키지가 entity 계층에 의존하지 않도록 한다 (매핑은 ChatService가 담당).
 */
public enum ChatSpeaker {
    USER, BOT
}
