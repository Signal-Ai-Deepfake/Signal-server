package com.signal.domain.chat.progress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.signal.domain.chat.engine.ChatCompletionClient;
import com.signal.domain.chat.engine.SituationType;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LlmStageAnalyzerTest {

    @Mock
    private ChatCompletionClient client;

    @Test
    void 코드블록으로_감싼_JSON도_파싱하고_NOT_APPLICABLE과_알수없는_값은_무시한다() {
        when(client.completeJson(anyString(), anyString())).thenReturn("""
                ```json
                {"stages": {"SITUATION_CHECK": "completed", "PLATFORM_REPORT": "IN_PROGRESS",
                 "EVIDENCE_PRESERVATION": "NOT_APPLICABLE", "AGENCY_GUIDANCE": "???"}}
                ```""");
        LlmStageAnalyzer analyzer = new LlmStageAnalyzer(client, new ObjectMapper(), true);

        Map<ConsultationStage, StageStatus> result =
                analyzer.analyze(SituationType.IMAGE_ABUSE, List.of(), Map.of());

        assertThat(result).containsOnly(
                Map.entry(ConsultationStage.SITUATION_CHECK, StageStatus.COMPLETED),
                Map.entry(ConsultationStage.PLATFORM_REPORT, StageStatus.IN_PROGRESS));
    }

    @Test
    void 유효한_단계가_하나도_없으면_예외를_던진다() {
        when(client.completeJson(anyString(), anyString())).thenReturn("{\"foo\": 1}");
        LlmStageAnalyzer analyzer = new LlmStageAnalyzer(client, new ObjectMapper(), true);

        assertThatThrownBy(() -> analyzer.analyze(SituationType.GENERAL, List.of(), Map.of()))
                .isInstanceOf(IllegalStateException.class);
    }
}
