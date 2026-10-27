package com.signal.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.signal.global.i18n.LanguageContext;
import com.signal.global.i18n.ResponseLanguage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

class AsyncConfigLanguageTest {

    @AfterEach
    void clearLanguage() {
        LanguageContext.clear();
    }

    private ResponseLanguage languageSeenInAsyncTask(ResponseLanguage languageOfRequest) throws Exception {
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) new AsyncConfig().taskExecutor();
        try {
            AtomicReference<ResponseLanguage> seenInTask = new AtomicReference<>();
            CountDownLatch done = new CountDownLatch(1);

            if (languageOfRequest != null) {
                LanguageContext.set(languageOfRequest);
            }
            executor.execute(() -> {
                seenInTask.set(LanguageContext.current());
                done.countDown();
            });
            LanguageContext.clear();

            assertThat(done.await(5, TimeUnit.SECONDS)).isTrue();
            return seenInTask.get();
        } finally {
            executor.shutdown();
        }
    }

    @Test
    void 비동기_작업에도_요청의_응답_언어가_전달된다() throws Exception {
        assertThat(languageSeenInAsyncTask(ResponseLanguage.JA)).isEqualTo(ResponseLanguage.JA);
    }

    @Test
    void 언어가_없던_요청에서_시작한_비동기_작업은_한국어다() throws Exception {
        assertThat(languageSeenInAsyncTask(null)).isEqualTo(ResponseLanguage.KO);
    }
}
