package com.signal.domain.chat.engine;

import com.signal.domain.chat.guidance.AgencyCatalog;
import com.signal.domain.chat.guidance.GuidanceText;
import com.signal.domain.chat.guidance.SafetySignals;
import com.signal.global.i18n.LanguageContext;
import com.signal.global.i18n.ResponseLanguage;
import java.util.List;

/**
 * 룰(키워드) 기반 챗 엔진.
 *
 * 더 이상 Spring 빈으로 직접 등록되지 않는다({@link LlmChatEngine} 참고). 위기(자해 등) 상황 감지,
 * 상황 분류(situationType), 추천 행동/상담 기관 목록은 정확성과 안전이 최우선이라 LLM에 맡기지 않고
 * 계속 이 결정론적 키워드 로직으로 산출하며, LlmChatEngine이 이 클래스를 내부적으로 위임 대상 겸
 * 폴백(LLM 미설정/호출 실패 시)으로 사용한다.
 *
 * 감지는 {@link SafetySignals}(한·영·일·중 키워드), 고정 문구는 {@link GuidanceText}, 기관 목록은
 * {@link AgencyCatalog}에 있고, 응답 언어는 요청의 Accept-Language({@link LanguageContext})를 따른다.
 * 키워드 매칭은 이번 메시지 하나만으로 충분하므로 history 파라미터는 사용하지 않는다.
 */
public class RuleBasedChatEngine implements ChatEngine {

    @Override
    public ChatEngineResponse respond(String content, List<ChatTurn> history) {
        ResponseLanguage language = LanguageContext.current();
        boolean crisisDetected = SafetySignals.isCrisis(content);
        SituationType situationType = SafetySignals.isImageAbuse(content)
                ? SituationType.IMAGE_ABUSE
                : SituationType.GENERAL;

        return new ChatEngineResponse(
                crisisDetected ? GuidanceText.crisisReply(language) : GuidanceText.fallbackReply(situationType, language),
                situationType,
                GuidanceText.suggestedActions(situationType, crisisDetected, language),
                crisisDetected,
                crisisDetected ? AgencyCatalog.crisis(language) : List.of());
    }
}
