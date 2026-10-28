package com.signal.domain.deepfakedetection.detector;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.signal.domain.deepfakedetection.entity.DetectionFailureReason;
import com.signal.domain.deepfakedetection.entity.Verdict;
import com.signal.domain.deepfakedetection.repository.DeepfakeDetectionRepository;
import com.signal.global.file.FileStorage;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * 전용 탐지 모델 추론 서비스(detect-lab, FastAPI)의 {@code POST /detect}를 호출하는 탐지기.
 * {@code deepfake-detection.mode=http} 일 때만 활성화되며, 기본값(llm)에서는 {@link LlmDeepfakeDetector}가 사용된다.
 *
 * 점수(score, 0~1)는 모델이 낸 "가짜일 확률"이고 판정은 점수만으로 정한다. LLM은 판정에 관여하지 않는다.
 * riskScore는 score를 0~100으로 옮긴 값이고, verdict는 기존 구간(30 미만 REAL, 70 이하 SUSPICIOUS, 그 위 FAKE)을 그대로 쓴다.
 * confidence는 모델이 고른 쪽의 확률(max(score, 1 - score))이며 보정되지 않은 값이다.
 * 구간 임계값을 평가 결과로 정하고 "판단 불가"를 두는 일은 별도 작업(#37)이다.
 *
 * 이 모델은 얼굴 검출을 하지 않아 NO_FACE_DETECTED를 내지 않고, 근거 영역(evidences)도 내지 않는다(빈 목록).
 * 영상은 지원하지 않는다. 실패(연결 불가, 타임아웃, 비정상 응답, 범위 밖 점수)는 룰 기반 값으로 대체하지 않고
 * FAILED(분석하지 못함)로 처리한다.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "deepfake-detection.mode", havingValue = "http")
public class HttpDeepfakeDetector implements DeepfakeDetector {

    private static final String HIGHLIGHTED_RESULT_DIRECTORY = "deepfake-detections";

    private final FileStorage fileStorage;
    private final StubDeepfakeDetector resultWriter;
    private final RestClient restClient;

    @Autowired
    public HttpDeepfakeDetector(FileStorage fileStorage,
                                DeepfakeDetectionRepository deepfakeDetectionRepository,
                                RestClient.Builder restClientBuilder,
                                @Value("${deepfake-detection.http.base-url:http://localhost:8000}") String baseUrl,
                                @Value("${deepfake-detection.http.timeout-seconds:60}") long timeoutSeconds) {
        this(fileStorage, deepfakeDetectionRepository, buildClient(restClientBuilder, baseUrl, timeoutSeconds));
    }

    HttpDeepfakeDetector(FileStorage fileStorage, DeepfakeDetectionRepository deepfakeDetectionRepository,
                         RestClient restClient) {
        this.fileStorage = fileStorage;
        // 완료/실패 기록과 verdict 구간 계산은 기존 구현을 그대로 쓴다(지연은 쓰지 않는다).
        this.resultWriter = new StubDeepfakeDetector(fileStorage, deepfakeDetectionRepository, 0);
        this.restClient = restClient;
    }

    private static RestClient buildClient(RestClient.Builder builder, String baseUrl, long timeoutSeconds) {
        // 전역 http.client.read-timeout(10초)은 첫 추론에 짧을 수 있어 이 클라이언트만 따로 설정한다.
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));
        return builder.baseUrl(baseUrl).requestFactory(factory).build();
    }

    @Override
    @Async
    public void detect(Long detectionId, String fileUrl, boolean isVideo) {
        if (isVideo) {
            log.info("영상은 딥페이크 탐지를 지원하지 않습니다: detectionId={}", detectionId);
            resultWriter.markFailed(detectionId, DetectionFailureReason.VIDEO_NOT_SUPPORTED);
            return;
        }
        try {
            byte[] content = fileStorage.load(fileUrl);

            DetectResponse response = restClient.post()
                    .uri("/detect")
                    .body(multipartBody(content, fileUrl))
                    .retrieve()
                    .body(DetectResponse.class);

            double score = validatedScore(response);
            int riskScore = (int) Math.round(score * 100);
            Verdict verdict = resultWriter.toVerdict(riskScore);
            double confidence = Math.round(Math.max(score, 1 - score) * 100) / 100.0;
            String highlightedResultUrl = fileStorage.store(content, "highlighted.png", HIGHLIGHTED_RESULT_DIRECTORY);

            log.info("딥페이크 탐지 완료: detectionId={}, model={}, modelVersion={}, score={}",
                    detectionId, response.model(), response.modelVersion(), score);
            resultWriter.markCompleted(
                    detectionId, verdict, confidence, riskScore, List.of(), highlightedResultUrl, false);
        } catch (Exception e) {
            DetectionFailureReason reason = isTimeout(e)
                    ? DetectionFailureReason.TIMEOUT
                    : DetectionFailureReason.AI_SERVER_ERROR;
            log.warn("딥페이크 탐지 서비스 호출 실패, 분석하지 못함으로 처리합니다: detectionId={}, reason={}",
                    detectionId, reason, e);
            resultWriter.markFailed(detectionId, reason);
        }
    }

    private static double validatedScore(DetectResponse response) {
        if (response == null || response.score() == null) {
            throw new IllegalStateException("탐지 서비스 응답에 score가 없습니다.");
        }
        double score = response.score();
        if (Double.isNaN(score) || score < 0.0 || score > 1.0) {
            throw new IllegalStateException("탐지 서비스 score가 0~1 범위를 벗어났습니다: " + score);
        }
        return score;
    }

    private static MultiValueMap<String, Object> multipartBody(byte[] bytes, String fileUrl) {
        String filename = filenameOf(fileUrl);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return filename;
            }
        });
        return body;
    }

    private static String filenameOf(String url) {
        if (url == null || url.isBlank()) {
            return "image";
        }
        int slash = url.lastIndexOf('/');
        String name = slash >= 0 ? url.substring(slash + 1) : url;
        return name.isBlank() ? "image" : name;
    }

    private static boolean isTimeout(Throwable e) {
        for (Throwable cause = e; cause != null; cause = cause.getCause()) {
            if (cause instanceof SocketTimeoutException || cause instanceof HttpTimeoutException) {
                return true;
            }
        }
        return false;
    }

    /** detect-lab의 POST /detect 응답. 모르는 필드(width, height 등)는 무시한다. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record DetectResponse(Double score, String model, String modelVersion) {
    }
}
