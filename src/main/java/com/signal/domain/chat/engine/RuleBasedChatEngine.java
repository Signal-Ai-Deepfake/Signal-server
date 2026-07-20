package com.signal.domain.chat.engine;

import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 실제 AI 서버 연동 전까지 사용하는 룰(키워드) 기반 스텁 구현체. 실서버 연동 시 이 클래스를 교체한다.
 */
@Component
public class RuleBasedChatEngine implements ChatEngine {

    private static final List<String> ABUSE_KEYWORDS = List.of("도용", "유포");

    private static final List<String> CRISIS_KEYWORDS = List.of(
            "자살", "죽고 싶", "죽고싶", "자해", "극단적 선택", "삶을 포기");

    private static final List<String> CRISIS_AGENCIES = List.of(
            "자살예방상담전화 (국번없이 1393)",
            "정신건강 위기상담전화 (1577-0199)",
            "청소년전화 1388");

    @Override
    public ChatEngineResponse respond(String content) {
        boolean crisisDetected = containsAny(content, CRISIS_KEYWORDS);
        boolean abuseDetected = containsAny(content, ABUSE_KEYWORDS);
        SituationType situationType = abuseDetected ? SituationType.IMAGE_ABUSE : SituationType.GENERAL;

        return new ChatEngineResponse(
                buildReply(situationType, crisisDetected),
                situationType,
                buildSuggestedActions(situationType, crisisDetected),
                crisisDetected,
                crisisDetected ? CRISIS_AGENCIES : List.of());
    }

    private boolean containsAny(String content, List<String> keywords) {
        return keywords.stream().anyMatch(content::contains);
    }

    private String buildReply(SituationType situationType, boolean crisisDetected) {
        if (crisisDetected) {
            return "많이 힘드셨겠어요. 지금 혼자가 아니라는 걸 꼭 기억해주세요. "
                    + "아래 상담 기관에 연락하시면 도움을 받으실 수 있어요.";
        }
        if (situationType == SituationType.IMAGE_ABUSE) {
            return "이미지 도용·유포 피해는 신속한 대응이 중요해요. 아래 조치를 참고해주세요.";
        }
        return "말씀해주셔서 감사해요. 조금 더 자세히 상황을 알려주시면 도와드릴게요.";
    }

    private List<String> buildSuggestedActions(SituationType situationType, boolean crisisDetected) {
        if (crisisDetected) {
            return List.of(
                    "전문 상담기관에 즉시 연락하기",
                    "신뢰할 수 있는 사람에게 지금 상황을 알리기",
                    "혼자 있지 않고 주변에 도움 요청하기");
        }
        if (situationType == SituationType.IMAGE_ABUSE) {
            return List.of(
                    "유포된 게시물의 URL과 화면을 캡처해 증거 남기기",
                    "게시된 플랫폼에 신고 및 삭제 요청하기",
                    "필요 시 사이버수사대(경찰청 사이버범죄 신고시스템)에 신고하기");
        }
        return List.of("어떤 상황인지 조금 더 자세히 알려주세요");
    }
}
