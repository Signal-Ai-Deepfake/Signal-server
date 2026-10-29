package com.signal.global.i18n;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class LanguageContextFilterTest {

    private final LanguageContextFilter filter = new LanguageContextFilter();

    private ResponseLanguage languageSeenByChain(String acceptLanguage) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (acceptLanguage != null) {
            request.addHeader("Accept-Language", acceptLanguage);
        }
        AtomicReference<ResponseLanguage> seen = new AtomicReference<>();

        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> seen.set(LanguageContext.current()));

        return seen.get();
    }

    @Test
    void 요청의_Accept_Language를_체인_안에서_사용할_수_있다() throws Exception {
        assertThat(languageSeenByChain("en")).isEqualTo(ResponseLanguage.EN);
        assertThat(languageSeenByChain("ja")).isEqualTo(ResponseLanguage.JA);
        assertThat(languageSeenByChain("zh-CN")).isEqualTo(ResponseLanguage.ZH_CN);
    }

    @Test
    void 헤더가_없으면_한국어다() throws Exception {
        assertThat(languageSeenByChain(null)).isEqualTo(ResponseLanguage.KO);
    }

    @Test
    void 요청이_끝나면_값을_정리해_다음_요청에_남지_않는다() throws Exception {
        languageSeenByChain("en");

        assertThat(LanguageContext.current()).isEqualTo(ResponseLanguage.KO);
    }

    @Test
    void 응답은_Accept_Language에_따라_달라지므로_Vary_헤더를_붙인다() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(new MockHttpServletRequest(), response, (req, res) -> { });

        assertThat(response.getHeaders("Vary")).contains("Accept-Language");
    }
}
