package com.signal.global.i18n;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * API 응답(AI가 쓰는 문장 포함)에 사용할 언어. 요청의 Accept-Language 헤더로 결정하며, 헤더가 없거나
 * 지원하지 않는 언어이면 한국어(KO)를 사용한다.
 *
 * 지원 언어: ko, en, ja, zh-CN(중국어 간체). 번체(zh-TW, zh-HK, zh-Hant)는 지원하지 않는다.
 */
public enum ResponseLanguage {

    KO("ko", null),
    EN("en", "English"),
    JA("ja", "Japanese"),
    ZH_CN("zh-CN", "Simplified Chinese");

    private final String tag;
    private final String promptLanguageName;

    ResponseLanguage(String tag, String promptLanguageName) {
        this.tag = tag;
        this.promptLanguageName = promptLanguageName;
    }

    /** 프론트가 보내는 값과 같은 언어 태그 (ko, en, ja, zh-CN). */
    public String getTag() {
        return tag;
    }

    /**
     * AI 프롬프트 뒤에 붙일 언어 지시문. 한국어는 기존 프롬프트가 이미 한국어를 지시하므로 빈 문자열이다.
     * JSON으로 답하는 프롬프트에서도 키와 고정 식별자 값(verdict, type 등)은 번역되지 않도록 명시한다.
     */
    public String promptInstruction() {
        if (promptLanguageName == null) {
            return "";
        }
        return "\n\n[Response language]\n"
                + "Ignore any earlier instruction that fixes the output language to Korean. "
                + "Write ALL user-facing natural-language text (notes, descriptions, replies) in "
                + promptLanguageName + ". "
                + "If you must answer in JSON, keep the JSON keys and the fixed identifier values "
                + "(such as verdict, type and other enum-like values) exactly as specified, untranslated.";
    }

    /**
     * Accept-Language 헤더 값에서 지원 언어를 고른다. 선호도(q) 순서대로 보며, 지원하는 첫 언어를 반환한다.
     * 헤더가 없거나 비어 있거나 지원 언어가 하나도 없으면 KO.
     */
    public static ResponseLanguage fromAcceptLanguage(String header) {
        if (header == null || header.isBlank()) {
            return KO;
        }

        List<Candidate> candidates = new ArrayList<>();
        int order = 0;
        for (String part : header.split(",")) {
            String[] pieces = part.trim().split(";");
            if (pieces.length == 0) {
                continue;
            }
            String rangeTag = pieces[0].trim().toLowerCase(Locale.ROOT);
            if (rangeTag.isEmpty()) {
                continue;
            }
            double quality = 1.0;
            for (int i = 1; i < pieces.length; i++) {
                String parameter = pieces[i].trim();
                if (parameter.startsWith("q=")) {
                    try {
                        quality = Double.parseDouble(parameter.substring(2).trim());
                    } catch (NumberFormatException e) {
                        quality = 0.0;
                    }
                }
            }
            candidates.add(new Candidate(rangeTag, quality, order++));
        }

        candidates.sort((a, b) -> {
            int byQuality = Double.compare(b.quality, a.quality);
            return byQuality != 0 ? byQuality : Integer.compare(a.order, b.order);
        });

        for (Candidate candidate : candidates) {
            if (candidate.quality <= 0) {
                continue;
            }
            ResponseLanguage matched = match(candidate.rangeTag);
            if (matched != null) {
                return matched;
            }
        }
        return KO;
    }

    private static ResponseLanguage match(String rangeTag) {
        if (rangeTag.equals("ko") || rangeTag.startsWith("ko-")) {
            return KO;
        }
        if (rangeTag.equals("en") || rangeTag.startsWith("en-")) {
            return EN;
        }
        if (rangeTag.equals("ja") || rangeTag.startsWith("ja-")) {
            return JA;
        }
        if (rangeTag.equals("zh") || rangeTag.equals("zh-cn") || rangeTag.equals("zh-sg")
                || rangeTag.startsWith("zh-hans")) {
            return ZH_CN;
        }
        return null;
    }

    private record Candidate(String rangeTag, double quality, int order) {
    }
}
