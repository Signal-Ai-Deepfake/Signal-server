package com.signal.global.i18n;

/**
 * 현재 요청(또는 그 요청에서 시작된 비동기 작업)의 응답 언어를 스레드 단위로 보관한다.
 * 요청 스레드에서는 {@link LanguageContextFilter}가, 비동기 스레드에서는 AsyncConfig의 TaskDecorator가
 * 값을 채우고 정리한다. 값이 없으면 한국어(KO)다.
 */
public final class LanguageContext {

    private static final ThreadLocal<ResponseLanguage> CURRENT = new ThreadLocal<>();

    private LanguageContext() {
    }

    public static ResponseLanguage current() {
        ResponseLanguage language = CURRENT.get();
        return language != null ? language : ResponseLanguage.KO;
    }

    public static void set(ResponseLanguage language) {
        CURRENT.set(language);
    }

    public static void clear() {
        CURRENT.remove();
    }
}
