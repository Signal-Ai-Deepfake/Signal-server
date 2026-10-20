package com.signal.global.ai;

import java.util.regex.Pattern;

/**
 * LLM에게 "순수 JSON만 응답하라"고 지시해도 ```json ... ``` 코드블록, Qwen3 계열의 &lt;think&gt; 추론
 * 블록, JSON 앞뒤의 설명 문장을 붙여 응답하는 경우가 있어, JSON 파싱 전에 이를 제거하는 공용 유틸리티.
 */
public final class LlmJsonExtractor {

    private static final Pattern THINK_BLOCK = Pattern.compile("(?s)<think>.*?</think>");

    private LlmJsonExtractor() {
    }

    public static String stripCodeFence(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = THINK_BLOCK.matcher(raw).replaceAll("").trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline != -1) {
                trimmed = trimmed.substring(firstNewline + 1);
            }
            int lastFence = trimmed.lastIndexOf("```");
            if (lastFence != -1) {
                trimmed = trimmed.substring(0, lastFence);
            }
            trimmed = trimmed.trim();
        }
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start > 0 && end > start) {
            trimmed = trimmed.substring(start, end + 1);
        }
        return trimmed.trim();
    }
}
