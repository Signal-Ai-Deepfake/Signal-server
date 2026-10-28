package com.signal.domain.chat.guidance;

import com.signal.global.i18n.ResponseLanguage;
import java.util.List;

/**
 * 챗봇이 안내하는 기관 목록. 연락처는 사람이 정기적으로 확인해야 하는 데이터라 한곳에 모아 둔다.
 * 검증 근거와 확인하지 못한 항목은 docs/chat-guidance.md에 적어 두었다. 번호를 바꿀 때는 이 파일만 고친다.
 *
 * 마지막 검토일: 2026-10-28 (공식 보도자료·정부 공지 기준, 일부 항목은 릴리스 전 재확인 필요)
 */
public final class AgencyCatalog {

    public static final String LAST_REVIEWED = "2026-10-28";

    /** 2024-01부터 1393 등이 109로 통합됐다(보건복지부 공지). */
    private static final Agency SUICIDE_PREVENTION = new Agency("suicide-prevention",
            "자살예방 상담전화", "Suicide Prevention Hotline (Korea)",
            "自殺予防相談電話(韓国)", "自杀预防咨询热线(韩国)", "109");

    private static final Agency YOUTH_LINE = new Agency("youth-1388",
            "청소년전화", "Youth Helpline (Korea)",
            "青少年電話(韓国)", "青少年热线(韩国)", "1388");

    private static final Agency EMERGENCY = new Agency("emergency",
            "긴급 상황(경찰·소방)", "Emergency (police / fire)",
            "緊急(警察・消防)", "紧急情况(警察/消防)", "112 / 119");

    private static final Agency DIGITAL_SEX_CRIME_CENTER = new Agency("d4u",
            "중앙디지털성범죄피해자지원센터", "Digital Sexual Crime Victim Support Center (Korea)",
            "中央デジタル性犯罪被害者支援センター(韓国)", "中央数字性犯罪受害者支援中心(韩国)", "02-735-8994 / d4u.stop.or.kr");

    private static final Agency KCSC = new Agency("kcsc",
            "방송통신심의위원회 불법촬영물 심의", "Korea Communications Standards Commission",
            "放送通信審議委員会(韓国)", "韩国放送通信审议委员会", "1377");

    private static final Agency POLICE = new Agency("police",
            "경찰 신고", "Police report",
            "警察への通報", "向警方报案", "112 / ecrm.police.go.kr");

    private AgencyCatalog() {
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

    private static List<String> format(ResponseLanguage language, Agency... agencies) {
        return java.util.Arrays.stream(agencies).map(agency -> agency.format(language)).toList();
    }
}
