package com.signal.domain.chat.guidance;

import java.util.List;
import java.util.Locale;

/**
 * 메시지에서 위기(자해·자살) 신호와 이미지 도용·유포 신호를 감지하는 결정론적 키워드 사전.
 *
 * 응답 언어(Accept-Language)와 무관하게 한국어·영어·일본어·중국어(간체) 키워드를 모두 검사한다.
 * 사용자가 헤더와 다른 언어로 쓸 수 있고, 안전 신호는 놓치는 쪽이 더 위험하기 때문이다.
 * 위기 쪽은 오탐(불필요하게 안내가 나가는 것)보다 미탐을 더 나쁘게 보고 넓게 잡는다.
 * 키워드 매칭은 완전하지 않으므로, 이 사전은 "감지"의 하한선이지 의료적 판단이 아니다.
 */
public final class SafetySignals {

    private static final List<String> CRISIS = List.of(
            // ko
            "자살", "죽고 싶", "죽고싶", "자해", "극단적 선택", "삶을 포기", "살기 싫", "사라지고 싶",
            "포기하고 싶", "죽어버리", "죽을래", "목숨을 끊",
            // en
            "suicide", "suicidal", "kill myself", "killing myself", "want to die", "wanna die",
            "end my life", "take my own life", "self-harm", "self harm", "hurt myself", "cut myself",
            "don't want to live", "dont want to live", "no reason to live", "better off dead",
            // ja
            "死にたい", "自殺", "消えたい", "自傷", "リストカット", "生きていたくない", "生きたくない", "死のう",
            // zh-CN
            "自杀", "想死", "不想活", "自残", "轻生", "结束生命", "活不下去", "了结自己");

    /** 그 자체로 이미지 피해를 가리키는 표현. 단독으로도 이미지 피해로 분류한다. */
    private static final List<String> ABUSE_STRONG = List.of(
            // ko
            "딥페이크", "몸캠", "불법 촬영", "불법촬영", "합성사진", "리벤지", "성착취",
            // en
            "deepfake", "deep fake", "fake nude", "fake porn", "nude photo", "nudes", "revenge porn",
            "sextortion", "face swap",
            // ja
            "ディープフェイク", "リベンジポルノ", "盗撮", "性的な画像",
            // zh-CN
            "深度伪造", "换脸", "裸照", "偷拍", "私密照");

    /** 일상 대화에도 나오는 표현. 사진·영상 같은 이미지 단어와 함께 나올 때만 이미지 피해로 분류한다. */
    private static final List<String> ABUSE_AMBIGUOUS = List.of(
            // ko
            "도용", "유포", "합성", "협박", "동의 없이",
            // en
            "leaked my", "blackmail", "without my consent", "morphed",
            // ja
            "合成", "流出", "拡散", "脅迫", "無断",
            // zh-CN
            "合成", "泄露", "传播", "威胁", "勒索", "未经同意");

    private static final List<String> IMAGE_WORDS = List.of(
            // ko
            "사진", "영상", "이미지", "얼굴", "촬영", "동영상", "짤",
            // en
            "photo", "picture", "image", "video", "pic ", "selfie", "face", "nude",
            // ja
            "写真", "画像", "動画", "顔", "映像",
            // zh-CN
            "照片", "图片", "图像", "视频", "脸", "影像", "相片");

    private SafetySignals() {
    }

    public static boolean isCrisis(String content) {
        return containsAny(content, CRISIS);
    }

    public static boolean isImageAbuse(String content) {
        return containsAny(content, ABUSE_STRONG)
                || (containsAny(content, ABUSE_AMBIGUOUS) && containsAny(content, IMAGE_WORDS));
    }

    private static boolean containsAny(String content, List<String> keywords) {
        if (content == null || content.isBlank()) {
            return false;
        }
        String normalized = content.toLowerCase(Locale.ROOT).replace('’', '\'');
        return keywords.stream().anyMatch(normalized::contains);
    }
}
