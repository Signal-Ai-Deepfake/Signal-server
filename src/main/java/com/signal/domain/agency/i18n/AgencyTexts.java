package com.signal.domain.agency.i18n;

import com.signal.global.i18n.ResponseLanguage;
import java.util.List;
import java.util.Map;

/**
 * 기관 조회 API 응답의 안내 문구(기관명, 운영시간, 지원 내용)를 요청 언어로 바꾸는 표.
 *
 * 기관 데이터는 한국어로 DB(data.sql)에 있고, 언어별 문구는 한국어 원문을 키로 찾는다. 키에 없는 값
 * (운영에서 직접 고쳤거나 새로 추가한 기관)은 번역하지 않고 DB 값 그대로 내려가므로, 번역이 낡아서 틀린 정보가
 * 나가는 일은 없다. 새 기관·문구를 시드에 추가하면 여기에도 한 줄 추가한다.
 *
 * 기관명은 연락·방문 때 한국어 명칭이 필요하므로 한국어 정식 명칭을 유지하고 번역을 괄호로 덧붙인다.
 * 전화번호·웹사이트는 언어와 무관하므로 다루지 않는다.
 */
public final class AgencyTexts {

    private record Translation(String en, String ja, String zh) {

        String in(ResponseLanguage language) {
            return switch (language) {
                case EN -> en;
                case JA -> ja;
                case ZH_CN -> zh;
                case KO -> null;
            };
        }
    }

    private static final Map<String, Translation> NAMES = Map.of(
            "디지털성범죄피해자지원센터", new Translation(
                    "Digital Sexual Crime Victim Support Center", "デジタル性犯罪被害者支援センター", "数字性犯罪受害者支援中心"),
            "경찰 사이버범죄 신고", new Translation(
                    "Police Cyber Crime Report", "警察サイバー犯罪通報", "警方网络犯罪举报"),
            "자살예방 상담전화", new Translation(
                    "Suicide Prevention Hotline", "自殺予防相談電話", "自杀预防咨询热线"),
            "정신건강 위기상담전화", new Translation(
                    "Mental Health Crisis Hotline", "精神健康危機相談電話", "心理健康危机咨询热线"));

    private static final Map<String, Translation> HOURS = Map.of(
            "평일 08:00~22:00, 주말·공휴일 09:00~18:00", new Translation(
                    "Weekdays 08:00-22:00, weekends and holidays 09:00-18:00",
                    "平日 08:00~22:00、土日・祝日 09:00~18:00",
                    "工作日 08:00~22:00,周末及节假日 09:00~18:00"),
            "평일 09:00~18:00 (야간·휴일 자동응답)", new Translation(
                    "Weekdays 09:00-18:00 (automated response at night and on holidays)",
                    "平日 09:00~18:00(夜間・休日は自動応答)",
                    "工作日 09:00~18:00(夜间及节假日为自动应答)"),
            "온라인 신고 24시간 (112는 긴급 신고)", new Translation(
                    "Online reports 24 hours (112 is for emergencies)",
                    "オンライン通報は24時間(112は緊急通報)",
                    "线上举报24小时(112为紧急报警)"),
            "24시간", new Translation("24 hours", "24時間", "24小时"));

    private static final Map<String, Translation> ACTIONS = Map.ofEntries(
            Map.entry("삭제 지원", new Translation("Removal support", "削除支援", "删除支援")),
            Map.entry("법률 상담 연계", new Translation("Legal counseling referral", "法律相談の紹介", "转介法律咨询")),
            Map.entry("심리 상담 연계", new Translation("Psychological counseling referral", "心理相談の紹介", "转介心理咨询")),
            Map.entry("사이버 범죄 신고", new Translation("Cyber crime reporting", "サイバー犯罪の通報", "网络犯罪举报")),
            Map.entry("수사 요청", new Translation("Request an investigation", "捜査の依頼", "申请调查")),
            Map.entry("위기 상담", new Translation("Crisis counseling", "危機相談", "危机咨询")),
            Map.entry("긴급 개입", new Translation("Emergency intervention", "緊急介入", "紧急介入")),
            Map.entry("정신건강 위기 상담", new Translation(
                    "Mental health crisis counseling", "精神保健の危機相談", "心理健康危机咨询")));

    private AgencyTexts() {
    }

    /** 한국어 정식 명칭은 그대로 두고, 다른 언어에서는 번역을 괄호로 덧붙인다. */
    public static String name(String koreanName, ResponseLanguage language) {
        String translated = lookup(NAMES, koreanName, language);
        return translated == null ? koreanName : koreanName + " (" + translated + ")";
    }

    public static String availableHours(String koreanHours, ResponseLanguage language) {
        String translated = lookup(HOURS, koreanHours, language);
        return translated == null ? koreanHours : translated;
    }

    public static List<String> supportedActions(List<String> koreanActions, ResponseLanguage language) {
        if (koreanActions == null) {
            return null;
        }
        return koreanActions.stream()
                .map(action -> {
                    String translated = lookup(ACTIONS, action, language);
                    return translated == null ? action : translated;
                })
                .toList();
    }

    private static String lookup(Map<String, Translation> table, String koreanText, ResponseLanguage language) {
        if (koreanText == null) {
            return null;
        }
        Translation translation = table.get(koreanText);
        return translation == null ? null : translation.in(language);
    }
}
