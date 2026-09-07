package com.signal.domain.chat.engine;

/**
 * LLM에게 대화 맥락을 넘겨주기 위한 과거 대화 한 턴. 현재 메시지(content 파라미터)는 포함하지 않고,
 * 그 이전에 오간 메시지들만 담는다.
 */
public record ChatTurn(ChatSpeaker speaker, String content) {
}
