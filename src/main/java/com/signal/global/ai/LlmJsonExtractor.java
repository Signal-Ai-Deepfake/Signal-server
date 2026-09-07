package com.signal.global.ai;

/**
 * LLM에게 "순수 JSON만 응답하라"고 지시해도 ```json ... ``` 코드블록으로 감싸서 응답하는 경우가 있어,
 * JSON 파싱 전에 이를 제거하는 공용 유틸리티.
 */
public final class LlmJsonExtractor {

    private LlmJsonExtractor() {
    }

    public static String stripCodeFence(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline != -1) {
                trimmed = trimmed.substring(firstNewline + 1);
            }
            int lastFence = trimmed.lastIndexOf("```");
            if (lastFence != -1) {
                trimmed = trimmed.substring(0, lastFence);
            }
        }
        return trimmed.trim();
    }
}
