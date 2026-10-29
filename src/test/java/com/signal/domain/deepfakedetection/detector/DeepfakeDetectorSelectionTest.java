package com.signal.domain.deepfakedetection.detector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.signal.domain.deepfakedetection.repository.DeepfakeDetectionRepository;
import com.signal.global.ai.VisionCompletionClient;
import com.signal.global.file.FileStorage;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.client.RestClient;

/** deepfake-detection.mode에 따라 DeepfakeDetector 빈이 정확히 하나만 등록되는지 확인한다. */
class DeepfakeDetectorSelectionTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withBean(FileStorage.class, () -> mock(FileStorage.class))
            .withBean(DeepfakeDetectionRepository.class, () -> mock(DeepfakeDetectionRepository.class))
            .withBean(VisionCompletionClient.class, () -> mock(VisionCompletionClient.class))
            .withBean(ObjectMapper.class, ObjectMapper::new)
            .withBean(RestClient.Builder.class, RestClient::builder)
            .withUserConfiguration(LlmDeepfakeDetector.class, HttpDeepfakeDetector.class);

    @Test
    void mode를_주지_않으면_LLM_탐지기가_쓰인다() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(DeepfakeDetector.class);
            assertThat(context).hasSingleBean(LlmDeepfakeDetector.class);
            assertThat(context).doesNotHaveBean(HttpDeepfakeDetector.class);
        });
    }

    @Test
    void mode가_llm이면_LLM_탐지기가_쓰인다() {
        runner.withPropertyValues("deepfake-detection.mode=llm").run(context -> {
            assertThat(context).hasSingleBean(DeepfakeDetector.class);
            assertThat(context).hasSingleBean(LlmDeepfakeDetector.class);
        });
    }

    @Test
    void mode가_http이면_HTTP_탐지기가_쓰인다() {
        runner.withPropertyValues("deepfake-detection.mode=http").run(context -> {
            assertThat(context).hasSingleBean(DeepfakeDetector.class);
            assertThat(context).hasSingleBean(HttpDeepfakeDetector.class);
            assertThat(context).doesNotHaveBean(LlmDeepfakeDetector.class);
        });
    }
}
