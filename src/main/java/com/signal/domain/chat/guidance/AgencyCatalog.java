package com.signal.domain.chat.guidance;

import com.signal.global.i18n.ResponseLanguage;
import java.util.List;

/**
 * 챗봇이 안내하는 기관 목록. 연락처는 사람이 정기적으로 확인해야 하는 데이터라 한곳에 모아 둔다.
 * 위기 응답·안내 블록 문장({@link GuidanceText})에도 일부 번호가 들어 있어서, 그 번호가 이 목록에 있는 번호와
 * 일치하는지 GuidanceTextTest가 검증한다. 번호를 바꾸면 문장도 함께 고쳐야 한다.
 * 검증 근거와 확인하지 못한 항목은 docs/chat-guidance.md에 적어 두었다. 번호를 바꿀 때는 이 파일만 고친다.
 *
 * 마지막 검토일: 2026-10-28 (공식 보도자료·정부 공지 기준, 일부 항목은 릴리스 전 재확인 필요)
 */
public final class AgencyCatalog {

    public static final String LAST_REVIEWED = "2026-10-28";

    /** 2024-01부터 1393 등이 109로 통합됐다(보건복지부 공지). */
    private static final Agency SUICIDE_PREVENTION = new Agency("suicide-prevention",
            "자살예방 상담전화", "Suicide Prevention Hotline (Korea)",
            "自殺予防相談電話(韓国)", "自杀预防咨询热线(韩国)", "109", List.of("109"));

    private static final Agency YOUTH_LINE = new Agency("youth-1388",
            "청소년전화", "Youth Helpline (Korea)",
            "青少年電話(韓国)", "青少年热线(韩国)", "1388", List.of("1388"));

    private static final Agency EMERGENCY = new Agency("emergency",
            "긴급 상황(경찰·소방)", "Emergency (police / fire)",
            "緊急(警察・消防)", "紧急情况(警察/消防)", "112 / 119", List.of("112", "119"));

    private static final Agency DIGITAL_SEX_CRIME_CENTER = new Agency("d4u",
            "중앙디지털성범죄피해자지원센터", "Digital Sexual Crime Victim Support Center (Korea)",
            "中央デジタル性犯罪被害者支援センター(韓国)", "中央数字性犯罪受害者支援中心(韩国)", "02-735-8994 / d4u.stop.or.kr", List.of("02-735-8994"));

    private static final Agency KCSC = new Agency("kcsc",
            "방송통신심의위원회 불법촬영물 심의", "Korea Communications Standards Commission",
            "放送通信審議委員会(韓国)", "韩国放送通信审议委员会", "1377", List.of("1377"));

    private static final Agency POLICE = new Agency("police",
            "경찰 신고", "Police report",
            "警察への通報", "向警方报案", "ecrm.police.go.kr / 112", List.of("112"));

    private AgencyCatalog() {
    }

    /** 카탈로그에 있는 모든 기관. */
    public static List<Agency> all() {
        return List.of(SUICIDE_PREVENTION, YOUTH_LINE, EMERGENCY, DIGITAL_SEX_CRIME_CENTER, KCSC, POLICE);
    }

    /** 카탈로그 기관의 전화번호 원문(예: "02-735-8994"). 기관 조회 API 시드의 phone 값과 통째로 비교할 때 쓴다. */
    public static java.util.Set<String> phoneNumbers() {
        java.util.Set<String> numbers = new java.util.TreeSet<>();
        all().forEach(agency -> numbers.addAll(agency.phones()));
        return numbers;
    }

    /** 위기 상황에서 안내하는 곳. */
    public static List<String> crisis(ResponseLanguage language) {
        return format(language, SUICIDE_PREVENTION, YOUTH_LINE, EMERGENCY);
    }

    /** 이미지 도용·유포 피해에서 안내하는 곳. */
    public static List<String> imageAbuse(ResponseLanguage language) {
        return format(language, DIGITAL_SEX_CRIME_CENTER, KCSC, POLICE);
    }

    /** 일반 상담에서 마음이 힘들 때 안내하는 곳. */
    public static List<String> generalSupport(ResponseLanguage language) {
        return format(language, SUICIDE_PREVENTION, YOUTH_LINE);
    }

    /** 모든 목록에 나오는 3자리 이상 숫자. 문장에 쓰인 번호가 목록과 일치하는지 검증할 때 쓴다. */
    public static java.util.Set<String> knownNumbers() {
        java.util.Set<String> numbers = new java.util.TreeSet<>();
        java.util.regex.Pattern digits = java.util.regex.Pattern.compile("\\d{3,}");
        for (ResponseLanguage language : ResponseLanguage.values()) {
            for (java.util.List<String> list : List.of(crisis(language), imageAbuse(language), generalSupport(language))) {
                for (String agency : list) {
                    digits.matcher(agency).results().forEach(match -> numbers.add(match.group()));
                }
            }
        }
        return numbers;
    }

    private static List<String> format(ResponseLanguage language, Agency... agencies) {
        return java.util.Arrays.stream(agencies).map(agency -> agency.format(language)).toList();
    }
}
