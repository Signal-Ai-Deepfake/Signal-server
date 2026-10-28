package com.signal.domain.chat.guidance;

import com.signal.domain.chat.engine.SituationType;
import com.signal.domain.chat.progress.ConsultationStage;
import com.signal.domain.riskassessment.entity.RiskLevel;
import com.signal.global.i18n.ResponseLanguage;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 서버가 고정 문구로 내보내는 모든 텍스트(위기 응답, 단계별 안내 블록, 종료 제안, 추천 행동)의 4개 국어 표.
 * LLM이 쓰는 대화 본문과 달리 내용이 항상 같고, 번호·절차가 틀리지 않도록 사람이 검토한 문장만 둔다.
 */
public final class GuidanceText {

    private static final Pattern EN_CONFIRM = Pattern.compile(
            "\\b(yes|yeah|yep|ok|okay|sure|please do|finish|end it|that's all|thats all)\\b",
            Pattern.CASE_INSENSITIVE);
    // "네가", "응원", "그래도", "그래서"처럼 다른 단어의 일부인 경우는 긍정으로 보지 않는다.
    private static final Pattern KO_CONFIRM = Pattern.compile(
            "(?<![가-힣])(네|넵|응|좋아(요)?|그래(요)?)(?![가-힣])");
    // 모호하면 종료하지 않는 쪽으로 판단한다(부정문이 긍정 키워드와 함께 오는 경우를 막기 위해 넓게 잡았다).
    private static final Pattern EN_DECLINE = Pattern.compile(
            "n't|\\b(no|nope|nah|not|never|more|continue|keep going|wait)\\b", Pattern.CASE_INSENSITIVE);

    private GuidanceText() {
    }

    // ---------------------------------------------------------------- 위기·기본 응답

    public static String crisisReply(ResponseLanguage lang) {
        return pick(lang,
                "많이 힘드셨겠어요. 지금 혼자가 아니라는 걸 꼭 기억해주세요. "
                        + "아래 상담 기관에 연락하시면 도움을 받으실 수 있어요. "
                        + "지금 당장 위험하다면 112나 119에 바로 연락해주세요.",
                "That sounds incredibly hard, and you are not alone. "
                        + "Please reach out to one of the services below. "
                        + "If you are in immediate danger, call 112 or 119 right now.",
                "とてもつらい思いをされているのですね。あなたは一人ではありません。"
                        + "下の相談窓口に連絡してみてください。今すぐ危険な状況なら、112か119にすぐ連絡してください。",
                "你一定非常难受,请记住你并不孤单。请联系下面的咨询机构获得帮助。"
                        + "如果现在就有危险,请立即拨打112或119。");
    }

    public static String fallbackReply(SituationType type, ResponseLanguage lang) {
        if (type == SituationType.IMAGE_ABUSE) {
            return pick(lang,
                    "이미지 도용·유포 피해는 신속한 대응이 중요해요. 아래 조치를 참고해주세요.",
                    "Quick action matters when your images have been misused or spread. Please see the steps below.",
                    "画像の悪用・拡散は、早めの対応が大切です。下の対応を参考にしてください。",
                    "图片被盗用或传播时,尽快处理很重要。请参考下面的步骤。");
        }
        return pick(lang,
                "말씀해주셔서 감사해요. 조금 더 자세히 상황을 알려주시면 도와드릴게요.",
                "Thank you for telling me. If you share a bit more about what happened, I can help.",
                "話してくださってありがとうございます。状況をもう少し詳しく教えていただければ、お手伝いします。",
                "谢谢你告诉我。如果能再详细说说情况,我可以帮到你。");
    }

    public static List<String> suggestedActions(SituationType type, boolean crisis, ResponseLanguage lang) {
        if (crisis) {
            return switch (lang) {
                case EN -> List.of("Contact a support service right away",
                        "Tell someone you trust what is happening now",
                        "Do not stay alone; ask someone nearby for help");
                case JA -> List.of("すぐに相談窓口へ連絡する",
                        "信頼できる人に今の状況を伝える",
                        "一人にならず、周りの人に助けを求める");
                case ZH_CN -> List.of("立即联系专业咨询机构",
                        "把现在的情况告诉你信任的人",
                        "不要独处,向身边的人求助");
                default -> List.of("전문 상담기관에 즉시 연락하기",
                        "신뢰할 수 있는 사람에게 지금 상황을 알리기",
                        "혼자 있지 않고 주변에 도움 요청하기");
            };
        }
        if (type == SituationType.IMAGE_ABUSE) {
            return switch (lang) {
                case EN -> List.of("Copy the URL and take screenshots as evidence",
                        "Report the post to the platform and request removal",
                        "If needed, report to the police cyber crime system");
                case JA -> List.of("投稿のURLと画面を保存して証拠を残す",
                        "掲載されたプラットフォームに通報して削除を依頼する",
                        "必要に応じて警察のサイバー犯罪通報システムに届け出る");
                case ZH_CN -> List.of("复制链接并截图保存证据",
                        "向发布平台举报并申请删除",
                        "必要时向警方网络犯罪举报系统报案");
                default -> List.of("유포된 게시물의 URL과 화면을 캡처해 증거 남기기",
                        "게시된 플랫폼에 신고 및 삭제 요청하기",
                        "필요 시 사이버수사대(경찰청 사이버범죄 신고시스템)에 신고하기");
            };
        }
        return List.of(pick(lang,
                "어떤 상황인지 조금 더 자세히 알려주세요",
                "Please tell me a little more about what is going on",
                "どのような状況か、もう少し詳しく教えてください",
                "请再详细说说是什么情况"));
    }

    // ---------------------------------------------------------------- 단계별 안내 블록

    /** 서버가 LLM 답변 뒤에 붙이는 안내 블록. 해당 단계에 블록이 없으면 null. */
    public static String guidanceBlock(ConsultationStage stage, SituationType type, ResponseLanguage lang) {
        return switch (stage) {
            case EVIDENCE_PRESERVATION -> pick(lang,
                    "가장 먼저 해두면 좋은 건 증거 보존이에요. 게시물이 지워지기 전에 ① 게시물 주소(URL)를 복사하고 "
                            + "② 게시일과 계정명이 보이게 화면을 캡처해 주세요. 삭제를 요청하기 전에 저장해 두는 게 중요해요.",
                    "The first thing worth doing is preserving evidence, before the post is taken down: "
                            + "(1) copy the post's URL and (2) take a screenshot that shows the date and account name. "
                            + "Save these before you ask for removal.",
                    "まず証拠の保存をおすすめします。投稿が消える前に、①投稿のURLをコピーし、"
                            + "②投稿日とアカウント名が見えるように画面を保存してください。削除を依頼する前に保存しておくことが大切です。",
                    "建议先保存证据。在帖子被删除之前,①复制帖子链接,②截图并确保能看到发布日期和账号名。"
                            + "在申请删除之前先保存很重要。");
            case PLATFORM_REPORT -> pick(lang,
                    "게시된 플랫폼의 신고 기능으로 삭제를 요청할 수 있어요. 방송통신심의위원회(1377)에도 심의를 신청할 수 있고, "
                            + "Signal의 신고 문서 기능으로 신고 내용을 정리해 둘 수도 있어요.",
                    "You can request removal through the platform's own report feature. You can also apply for review "
                            + "at the Korea Communications Standards Commission (1377), and Signal's report document "
                            + "feature can help you organize what to submit.",
                    "掲載されたプラットフォームの通報機能で削除を依頼できます。放送通信審議委員会(1377)に審議を申請することもでき、"
                            + "Signalの通報文書機能で内容を整理することもできます。",
                    "可以通过发布平台的举报功能申请删除。也可以向韩国放送通信审议委员会(1377)申请审议,"
                            + "还可以用Signal的举报文书功能整理举报内容。");
            case AGENCY_GUIDANCE -> type == SituationType.IMAGE_ABUSE
                    ? pick(lang,
                    "혼자 해결하려 하지 않으셔도 돼요. 디지털성범죄피해자지원센터(d4u.stop.or.kr)에서 상담과 삭제 지원을 신청할 수 있고, "
                            + "수사가 필요하면 경찰(112, 온라인은 사이버범죄 신고시스템)에 신고할 수 있어요.",
                    "You do not have to handle this alone. The Digital Sexual Crime Victim Support Center "
                            + "(d4u.stop.or.kr) accepts requests for counseling and removal support, and if an "
                            + "investigation is needed you can report to the police (112, or the online cyber crime reporting system).",
                    "一人で抱え込まなくて大丈夫です。デジタル性犯罪被害者支援センター(d4u.stop.or.kr)で相談や削除支援を申し込めます。"
                            + "捜査が必要な場合は警察(112、オンラインはサイバー犯罪通報システム)に届け出られます。",
                    "你不必独自面对。可以通过数字性犯罪受害者支援中心(d4u.stop.or.kr)申请咨询和删除支援,"
                            + "如需调查,可向警方(112,线上为网络犯罪举报系统)报案。")
                    : pick(lang,
                    "혼자 감당하기 어려울 땐 전문 상담을 받아보셔도 좋아요. 마음이 많이 힘들면 109(자살예방 상담전화)에, "
                            + "청소년이라면 1388에 연락할 수 있어요.",
                    "If it feels too heavy to carry alone, professional counseling can help. When things get very hard "
                            + "you can call 109 (suicide prevention line), or 1388 if you are a teenager.",
                    "一人で抱えきれないときは、専門の相談を受けてみてください。とてもつらいときは109(自殺予防相談電話)、"
                            + "青少年なら1388に連絡できます。",
                    "如果一个人难以承受,可以寻求专业咨询。非常难受时可以拨打109(自杀预防咨询热线),"
                            + "青少年可拨打1388。");
            case EMOTIONAL_SUPPORT -> pick(lang,
                    "그리고 지금 마음은 좀 어떠세요? 이런 일을 겪으면 불안하고 힘든 게 자연스러워요. "
                            + "하고 싶은 말이 있으면 편하게 해주세요.",
                    "And how are you feeling right now? Feeling anxious or overwhelmed after something like this is "
                            + "natural. Feel free to say whatever is on your mind.",
                    "それと、今の気持ちはいかがですか?こうしたことがあれば、不安になったりつらくなったりするのは自然なことです。"
                            + "話したいことがあれば、気軽に話してください。",
                    "还有,你现在心情怎么样?遇到这种事感到不安和难受是很自然的。想说什么都可以随时告诉我。");
            default -> null;
        };
    }

    // ---------------------------------------------------------------- 상담 요약

    public static String summarySituation(SituationType type, boolean crisis, ResponseLanguage lang) {
        if (type == null) {
            return pick(lang, "상담 시작 전", "Before the session", "相談開始前", "咨询开始前");
        }
        if (type == SituationType.IMAGE_ABUSE) {
            return crisis
                    ? pick(lang, "이미지 도용·유포로 인한 위기 상황",
                    "Crisis related to image misuse or spread",
                    "画像の悪用・拡散による危機的状況", "因图片被盗用或传播而陷入的危机")
                    : pick(lang, "이미지 도용·유포 피해 상담",
                    "Counseling about image misuse or spread",
                    "画像の悪用・拡散被害の相談", "图片被盗用或传播的受害咨询");
        }
        return crisis
                ? pick(lang, "정서적 위기 상황", "Emotional crisis", "精神的な危機的状況", "情绪危机")
                : pick(lang, "일반 상담", "General counseling", "一般相談", "一般咨询");
    }

    public static String summaryRiskDescription(RiskLevel level, ResponseLanguage lang) {
        return switch (level) {
            case HIGH -> pick(lang, "위험 신호가 감지되어 긴급한 조치가 필요해요.",
                    "Warning signs were detected and urgent action is needed.",
                    "危険な兆候が検知され、緊急の対応が必要です。", "检测到危险信号,需要紧急处理。");
            case MEDIUM -> pick(lang, "확산 여부를 확인하고 있어요.",
                    "We are checking whether the content has spread.",
                    "拡散しているかどうかを確認しています。", "正在确认内容是否已经扩散。");
            case LOW -> pick(lang, "특별한 위험 신호는 없어요.",
                    "No particular warning signs.",
                    "特に危険な兆候はありません。", "没有特别的危险信号。");
        };
    }

    /** 아직 남은 안내 단계를 사용자에게 보여 주는 짧은 이름. 안내 단계가 아니면 null. */
    public static String stepLabel(ConsultationStage stage, ResponseLanguage lang) {
        return switch (stage) {
            case EVIDENCE_PRESERVATION -> pick(lang, "증거 보존", "Preserve evidence", "証拠の保存", "保存证据");
            case PLATFORM_REPORT -> pick(lang, "플랫폼 신고", "Report to the platform", "プラットフォームへの通報", "向平台举报");
            case AGENCY_GUIDANCE -> pick(lang, "전문 기관 상담", "Contact a support organization", "専門機関への相談", "联系专业机构");
            default -> null;
        };
    }

    // ---------------------------------------------------------------- 종료 제안

    public static String endPrompt(ResponseLanguage lang) {
        return pick(lang,
                "지금까지 상황을 잘 정리해주셨어요. 상담을 마무리해도 괜찮을까요? 더 이야기하고 싶으시면 편하게 말씀해주세요.",
                "You have gone through a lot today. Is it okay to wrap up this session? If you would like to keep talking, just say so.",
                "ここまで状況をよく整理してくださいました。相談を終わりにしてもよろしいですか?まだ話したいことがあれば、気軽に教えてください。",
                "到目前为止你已经把情况梳理得很清楚了。可以结束这次咨询吗?如果还想继续聊,随时告诉我。");
    }

    public static String endClosing(ResponseLanguage lang) {
        return pick(lang,
                "상담을 마무리할게요. 언제든 다시 찾아주세요.",
                "Let's wrap up here. You are welcome back any time.",
                "相談を終わります。いつでもまた来てください。",
                "那我们就先结束吧。随时欢迎再来。");
    }

    /** 종료 제안에 대한 답이 "끝내자"인지 판단한다. "더 얘기하고 싶어" 같은 부정이 섞이면 끝내지 않는다. */
    public static boolean isEndConfirmation(String content) {
        if (content == null) {
            return false;
        }
        String text = content.toLowerCase(java.util.Locale.ROOT);
        boolean decline = EN_DECLINE.matcher(text).find()
                || containsAny(text, "아니", "더 얘기", "더 이야기", "계속", "아직", "싫", "않", "말고", "못", "안 ",
                "まだ", "いいえ", "続け", "たくない", "じゃない", "ではない", "ません", "いや", "だめ",
                "还想", "还要", "继续", "不想", "不要", "不用", "不行", "不能", "不是", "没有", "别", "先不", "暂时不");
        if (decline) {
            return false;
        }
        return EN_CONFIRM.matcher(text).find()
                || KO_CONFIRM.matcher(text).find()
                || containsAny(text, "그만할래", "종료할래", "끝낼래", "마무리",
                "はい", "お願いします", "終わり", "大丈夫", "好的", "是的", "结束");
    }

    // ---------------------------------------------------------------- 내부

    private static boolean containsAny(String text, String... needles) {
        for (String needle : needles) {
            if (text.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static String pick(ResponseLanguage lang, String ko, String en, String ja, String zh) {
        return switch (lang) {
            case EN -> en;
            case JA -> ja;
            case ZH_CN -> zh;
            default -> ko;
        };
    }
}
