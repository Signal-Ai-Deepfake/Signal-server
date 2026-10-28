package com.signal.domain.chat.guidance;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LLM이 쓴 답변에 기관 목록에 없는 전화번호가 섞였는지 검사한다.
 *
 * 프롬프트로 번호를 말하지 말라고 지시해도 LLM이 기억에 의존해 번호를 지어낼 수 있어서, 서버가 마지막으로 한 번 더 막는다.
 * 기관 번호는 {@link AgencyCatalog}에 있는 것만 허용하고, 사용자가 메시지에 쓴 숫자는 되풀이해도 되므로 허용한다.
 * 연도(19xx·20xx)와 퍼센트는 번호가 아니므로 무시한다. 오탐이면 LLM 답변 대신 안전한 고정 답변이 나간다.
 */
public final class ReplyGuard {

    private static final Pattern DIGITS = Pattern.compile("(?<!\\d)\\d{3,}(?!\\d)");

    private ReplyGuard() {
    }

    public static boolean containsUnknownNumber(String reply, String userMessage) {
        if (reply == null || reply.isBlank()) {
            return false;
        }
        Set<String> allowed = new HashSet<>(AgencyCatalog.knownNumbers());
        allowed.addAll(numbersIn(userMessage));

        Matcher matcher = DIGITS.matcher(reply);
        while (matcher.find()) {
            String number = matcher.group();
            if (allowed.contains(number) || isYear(number) || followedByPercent(reply, matcher.end())) {
                continue;
            }
            return true;
        }
        return false;
    }

    private static Set<String> numbersIn(String text) {
        Set<String> numbers = new HashSet<>();
        if (text != null) {
            Matcher matcher = DIGITS.matcher(text);
            while (matcher.find()) {
                numbers.add(matcher.group());
            }
        }
        return numbers;
    }

    private static boolean isYear(String number) {
        return number.length() == 4 && (number.startsWith("19") || number.startsWith("20"));
    }

    private static boolean followedByPercent(String text, int end) {
        return end < text.length() && text.charAt(end) == '%';
    }
}
