package com.signal.domain.chat.progress;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.signal.domain.chat.engine.ChatCompletionClient;
import com.signal.domain.chat.engine.ChatSpeaker;
import com.signal.domain.chat.engine.ChatTurn;
import com.signal.domain.chat.engine.SituationType;
import com.signal.global.ai.LlmJsonExtractor;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * LLM에게 대화를 보여주고 단계별 안내 상태를 구조화(JSON)해 판단하게 한다. 진행률 계산은 하지 않는다.
 * 비활성화(chat.llm.enabled=false)되거나 호출/파싱에 실패하면 예외를 던져 호출부가 룰 기반 값으로 대체한다.
 */
@Slf4j
@Component
public class LlmStageAnalyzer implements StageAnalyzer {

    private static final String SYSTEM_PROMPT = """
            당신은 딥페이크 피해 상담 서비스 'Signal'의 상담 진행 평가자입니다.
            대화를 읽고, 상담 챗봇이 각 안내 단계를 실제로 얼마나 안내했는지 판단하세요.
            이것은 "안내가 얼마나 진행됐는지"이지 피해가 해결됐는지가 아닙니다.

            단계 정의:
            - SITUATION_CHECK: 사용자가 겪은 상황(무슨 일인지, 어디에 유포됐는지 등)을 파악했다.
            - EMOTIONAL_SUPPORT: 사용자의 감정을 살피고 공감·안정을 도왔다.
            - EVIDENCE_PRESERVATION: 증거(URL·캡처 등) 보존 방법을 안내했거나 사용자가 증거를 제시했다.
            - PLATFORM_REPORT: 게시 플랫폼 신고·삭제 요청 방법을 안내했다.
            - AGENCY_GUIDANCE: 필요한 전문·상담·수사 기관을 안내했다.

            상태 값:
            - COMPLETED: 해당 안내가 대화에서 실제로 이루어졌다.
            - IN_PROGRESS: 안내가 시작됐지만 아직 충분하지 않다.
            - NOT_STARTED: 아직 다루지 않았다.
            대화에 근거가 없으면 COMPLETED로 판단하지 마세요. 이미 COMPLETED인 단계는 되돌리지 마세요.

            아래 JSON 형식으로만 답하세요. 코드블록이나 다른 설명 없이 순수 JSON만 출력하세요.
            {"stages": {"SITUATION_CHECK": "...", "EMOTIONAL_SUPPORT": "...", "EVIDENCE_PRESERVATION": "...",
             "PLATFORM_REPORT": "...", "AGENCY_GUIDANCE": "..."}}
            """;

    private final ChatCompletionClient chatCompletionClient;
    private final ObjectMapper objectMapper;
    private final boolean llmEnabled;

    public LlmStageAnalyzer(ChatCompletionClient chatCompletionClient,
                            ObjectMapper objectMapper,
                            @Value("${chat.llm.enabled:true}") boolean llmEnabled) {
        this.chatCompletionClient = chatCompletionClient;
        this.objectMapper = objectMapper;
        this.llmEnabled = llmEnabled;
    }

    @Override
    public Map<ConsultationStage, StageStatus> analyze(
            SituationType type, List<ChatTurn> turns, Map<ConsultationStage, StageStatus> previous) {
        if (!llmEnabled) {
            throw new IllegalStateException("chat.llm.enabled=false");
        }
        String raw = chatCompletionClient.completeJson(SYSTEM_PROMPT, buildUserPrompt(type, turns, previous));
        return parse(raw);
    }

    private String buildUserPrompt(
            SituationType type, List<ChatTurn> turns, Map<ConsultationStage, StageStatus> previous) {
        StringBuilder sb = new StringBuilder();
        sb.append("상담 유형: ").append(type).append('\n');
        sb.append("필요한 단계: ").append(ConsultationStage.applicableTo(type)).append('\n');
        sb.append("직전 단계 상태: ").append(previous).append("\n\n[대화]\n");
        for (ChatTurn turn : turns) {
            sb.append(turn.speaker() == ChatSpeaker.USER ? "사용자: " : "챗봇: ")
                    .append(turn.content()).append('\n');
        }
        return sb.toString();
    }

    private Map<ConsultationStage, StageStatus> parse(String raw) {
        try {
            JsonNode stages = objectMapper.readTree(LlmJsonExtractor.stripCodeFence(raw)).path("stages");
            Map<ConsultationStage, StageStatus> result = new EnumMap<>(ConsultationStage.class);
            for (ConsultationStage stage : ConsultationStage.values()) {
                StageStatus status = toStatus(stages.path(stage.name()).asText(null));
                if (status != null) {
                    result.put(stage, status);
                }
            }
            if (result.isEmpty()) {
                throw new IllegalStateException("LLM 단계 응답에 유효한 단계가 없습니다.");
            }
            return result;
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("LLM 단계 응답 파싱 실패", e);
        }
    }

    // NOT_APPLICABLE은 상담 유형으로 결정되므로 LLM 값은 받지 않는다.
    private StageStatus toStatus(String value) {
        if (value == null) {
            return null;
        }
        try {
            StageStatus status = StageStatus.valueOf(value.trim().toUpperCase());
            return status == StageStatus.NOT_APPLICABLE ? null : status;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
