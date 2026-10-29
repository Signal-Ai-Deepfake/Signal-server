package com.signal.domain.deepfakedetection.detector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.signal.domain.deepfakedetection.entity.DetectionBand;
import com.signal.domain.deepfakedetection.entity.Evidence;
import com.signal.global.ai.VisionCompletionClient;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class LlmDeepfakeExplainerTest {

    private final VisionCompletionClient client = mock(VisionCompletionClient.class);
    private final LlmDeepfakeExplainer explainer = new LlmDeepfakeExplainer(client, new ObjectMapper());
    private final byte[] image = {1, 2, 3};

    @Test
    void 낮음과_판단_불가는_LLM을_부르지_않는다() {
        assertThat(explainer.explain(image, "image/jpeg", DetectionBand.LOW, 1000, 800)).isEmpty();
        assertThat(explainer.explain(image, "image/jpeg", DetectionBand.UNDETERMINED, 1000, 800)).isEmpty();

        verify(client, never()).complete(any(), anyString(), anyString(), anyString());
    }

    @Test
    void 정해진_등급을_프롬프트로_알려_주고_설명만_받는다() {
        when(client.complete(any(), anyString(), anyString(), anyString())).thenReturn("""
                {"observations": [
                  {"description": "얼굴 경계가 주변보다 흐릿하게 보일 수 있습니다.",
                   "region": {"x": 0.25, "y": 0.1, "width": 0.5, "height": 0.4}},
                  {"description": "배경의 글자 모양이 어색해 보일 수 있습니다.", "region": null}
                ]}
                """);

        List<Evidence> evidences = explainer.explain(image, "image/jpeg", DetectionBand.HIGH, 1000, 800);

        assertThat(evidences).hasSize(2);
        assertThat(evidences.get(0).getDescription()).isEqualTo("얼굴 경계가 주변보다 흐릿하게 보일 수 있습니다.");
        assertThat(evidences.get(0).getRegion().getX()).isEqualTo(250);
        assertThat(evidences.get(0).getRegion().getY()).isEqualTo(80);
        assertThat(evidences.get(0).getRegion().getWidth()).isEqualTo(500);
        assertThat(evidences.get(0).getRegion().getHeight()).isEqualTo(320);
        assertThat(evidences.get(1).getRegion()).isNotNull(); // 위치를 모르면 이미지 중심 영역
        ArgumentCaptor<String> system = ArgumentCaptor.forClass(String.class);
        verify(client).complete(any(), anyString(), system.capture(), anyString());
        assertThat(system.getValue()).contains("합성 가능성이 높음").contains("바꾸거나 다시 평가할 수 없습니다");
    }

    @Test
    void 불확실_등급도_설명한다() {
        when(client.complete(any(), anyString(), anyString(), anyString()))
                .thenReturn("{\"observations\": [{\"description\": \"조명이 한쪽으로 치우쳐 보일 수 있습니다.\"}]}");

        assertThat(explainer.explain(image, "image/png", DetectionBand.UNCERTAIN, 0, 0)).hasSize(1);
    }

    @Test
    void 단정_표현이_든_문장은_버린다() {
        when(client.complete(any(), anyString(), anyString(), anyString())).thenReturn("""
                {"observations": [
                  {"description": "이 이미지는 100% 합성입니다."},
                  {"description": "가짜입니다."},
                  {"description": "피부 질감이 균일해 보일 수 있습니다."}
                ]}
                """);

        List<Evidence> evidences = explainer.explain(image, "image/jpeg", DetectionBand.HIGH, 1000, 800);

        assertThat(evidences).extracting(Evidence::getDescription).containsExactly("피부 질감이 균일해 보일 수 있습니다.");
    }

    @Test
    void 최대_3개까지만_쓴다() {
        when(client.complete(any(), anyString(), anyString(), anyString())).thenReturn("""
                {"observations": [{"description": "하나"}, {"description": "둘"}, {"description": "셋"}, {"description": "넷"}]}
                """);

        assertThat(explainer.explain(image, "image/jpeg", DetectionBand.HIGH, 1000, 800)).hasSize(3);
    }

    @Test
    void 등급을_바꾸려는_필드가_있어도_무시한다() {
        when(client.complete(any(), anyString(), anyString(), anyString())).thenReturn("""
                {"verdict": "REAL", "riskScore": 0,
                 "observations": [{"description": "윤곽이 매끄럽게 보일 수 있습니다."}]}
                """);

        List<Evidence> evidences = explainer.explain(image, "image/jpeg", DetectionBand.HIGH, 1000, 800);

        assertThat(evidences).hasSize(1); // 반환 타입이 근거 목록뿐이라 등급을 바꿀 방법이 없다
    }

    @Test
    void LLM이_실패하거나_엉뚱한_응답이면_빈_목록() {
        when(client.complete(any(), anyString(), anyString(), anyString()))
                .thenThrow(new IllegalStateException("호출 실패"))
                .thenReturn("not json")
                .thenReturn("{\"observations\": null}");

        assertThat(explainer.explain(image, "image/jpeg", DetectionBand.HIGH, 1000, 800)).isEmpty();
        assertThat(explainer.explain(image, "image/jpeg", DetectionBand.HIGH, 1000, 800)).isEmpty();
        assertThat(explainer.explain(image, "image/jpeg", DetectionBand.HIGH, 1000, 800)).isEmpty();
    }
}
