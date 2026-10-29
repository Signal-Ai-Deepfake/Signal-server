package com.signal.domain.deepfakedetection.detector;

import com.signal.domain.deepfakedetection.entity.ProvenanceSignal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * 이미지 파일의 메타데이터에서 AI 생성 흔적을 찾는 결정론적 추출기. 모델을 쓰지 않는다.
 *
 * 확인하는 것:
 * - PNG 텍스트 청크(tEXt, iTXt): Stable Diffusion WebUI의 "parameters", ComfyUI의 "prompt"·"workflow", InvokeAI 메타데이터
 * - 파일 앞부분(EXIF, XMP, C2PA 매니페스트가 있는 영역)의 IPTC digitalSourceType 값과, 제작 도구 필드에 적힌 생성 도구 이름
 * - C2PA(JUMBF) 매니페스트의 존재 여부(서명은 검증하지 않는다)
 *
 * 한계: 메타데이터는 스크린샷, 재저장, 메신저 전송으로 사라진다. 그래서 신호가 없다는 것은 아무 의미도 없다.
 * 신호가 있을 때만 근거로 쓴다.
 *
 * 구분:
 * - 이미지가 AI로 생성됐다는 표시(trainedAlgorithmicMedia, 생성 전용 도구 이름)만 강한 신호다.
 * - 일부를 AI로 편집·합성했다는 표시(compositeWithTrainedAlgorithmicMedia, compositeSynthetic, Adobe Firefly)는
 *   실제 사진에 생성형 채우기를 쓴 경우에도 남으므로 약한 신호(AI_EDIT_DECLARED)로만 올린다.
 * - algorithmicMedia는 학습 모델 없이 알고리즘으로 만든 이미지(프랙탈, 절차적 생성 등)라 AI 생성 표시로 보지 않는다.
 *
 * 이름 검색은 파일 전체 바이트에서 찾지 않고, XMP의 CreatorTool과 C2PA의 softwareAgent 값 근처에서만 한다.
 * 본문이나 캡션에 도구 이름이 적혀 있다고 신호로 보지 않기 위해서다. EXIF의 Software 태그(바이너리)는 읽지 않는다.
 */
public final class ProvenanceSignalExtractor {

    /** 이름 검색을 하는 파일 앞부분 크기. 메타데이터와 C2PA 매니페스트는 파일 앞쪽에 있다. */
    static final int SCAN_LIMIT = 2 * 1024 * 1024;

    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};

    /** 이미지가 AI로 생성됐다는 IPTC 값. 대소문자까지 이 표기 그대로 찾는다(composite 값은 대문자 T로 시작해 겹치지 않는다). */
    private static final String AI_SOURCE_VALUE = "trainedAlgorithmicMedia";

    /** 일부를 AI로 편집·합성했다는 IPTC 값. */
    private static final List<String> AI_EDIT_VALUES = List.of("compositeWithTrainedAlgorithmicMedia", "compositeSynthetic");

    /** 생성 전용 도구. 제작 도구 필드에 있으면 강한 신호. */
    private static final List<String> GENERATOR_NAMES = List.of(
            "Midjourney", "DALL-E", "Stable Diffusion", "ComfyUI", "NovelAI", "Leonardo.Ai");

    /** 실제 사진 편집에도 쓰이는 도구. 제작 도구 필드에 있어도 약한 신호. */
    private static final List<String> EDIT_TOOL_NAMES = List.of("Adobe Firefly");

    /** 도구 이름을 찾을 필드. XMP의 xmp:CreatorTool과 C2PA 액션의 softwareAgent. */
    private static final List<String> TOOL_FIELDS = List.of("CreatorTool", "softwareAgent");

    /** 필드 이름 뒤로 이 길이 안에서 도구 이름을 찾는다(속성, 요소, JSON 객체 모두 값이 바로 뒤에 온다). */
    private static final int TOOL_FIELD_WINDOW = 200;

    private ProvenanceSignalExtractor() {
    }

    public static List<ProvenanceSignal> extract(byte[] content) {
        if (content == null || content.length < 16) {
            return List.of();
        }
        Set<ProvenanceSignal> found = EnumSet.noneOf(ProvenanceSignal.class);

        byte[] head = content.length > SCAN_LIMIT ? Arrays.copyOf(content, SCAN_LIMIT) : content;
        String text = new String(head, StandardCharsets.ISO_8859_1);
        if (text.contains(AI_SOURCE_VALUE)) {
            found.add(ProvenanceSignal.AI_SOURCE_DECLARED);
        }
        for (String value : AI_EDIT_VALUES) {
            if (text.contains(value)) {
                found.add(ProvenanceSignal.AI_EDIT_DECLARED);
                break;
            }
        }
        scanToolFields(text, found);
        if (text.contains("jumb") && text.contains("c2pa")) {
            found.add(ProvenanceSignal.C2PA_MANIFEST);
        }

        if (isPng(content)) {
            scanPngTextChunks(content, found);
        }
        return new ArrayList<>(found);
    }

    private static void scanToolFields(String text, Set<ProvenanceSignal> found) {
        for (String field : TOOL_FIELDS) {
            int from = 0;
            while (true) {
                int at = text.indexOf(field, from);
                if (at < 0) {
                    break;
                }
                int start = at + field.length();
                String window = text.substring(start, Math.min(text.length(), start + TOOL_FIELD_WINDOW));
                for (String name : GENERATOR_NAMES) {
                    if (window.contains(name)) {
                        found.add(ProvenanceSignal.GENERATOR_METADATA);
                    }
                }
                for (String name : EDIT_TOOL_NAMES) {
                    if (window.contains(name)) {
                        found.add(ProvenanceSignal.AI_EDIT_DECLARED);
                    }
                }
                from = start;
            }
        }
    }

    /** 강한 신호가 하나라도 있으면 true. */
    public static boolean hasStrongSignal(List<ProvenanceSignal> signals) {
        return signals != null && signals.stream().anyMatch(ProvenanceSignal::isStrong);
    }

    private static boolean isPng(byte[] content) {
        if (content.length < PNG_SIGNATURE.length) {
            return false;
        }
        for (int i = 0; i < PNG_SIGNATURE.length; i++) {
            if (content[i] != PNG_SIGNATURE[i]) {
                return false;
            }
        }
        return true;
    }

    private static void scanPngTextChunks(byte[] content, Set<ProvenanceSignal> found) {
        int pos = PNG_SIGNATURE.length;
        while (pos + 12 <= content.length) {
            long length = readUnsignedInt(content, pos);
            String type = new String(content, pos + 4, 4, StandardCharsets.ISO_8859_1);
            int dataStart = pos + 8;
            if (length < 0 || dataStart + length > content.length) {
                return;
            }
            int dataEnd = (int) (dataStart + length);
            if ("tEXt".equals(type)) {
                inspectText(content, dataStart, dataEnd, found);
            } else if ("iTXt".equals(type)) {
                inspectInternationalText(content, dataStart, dataEnd, found);
            } else if ("zTXt".equals(type)) {
                inspectKeywordOnly(content, dataStart, dataEnd, found);
            } else if ("IEND".equals(type)) {
                return;
            }
            pos = dataEnd + 4; // CRC
        }
    }

    private static void inspectText(byte[] content, int start, int end, Set<ProvenanceSignal> found) {
        int nul = indexOfZero(content, start, end);
        if (nul < 0) {
            return;
        }
        String keyword = new String(content, start, nul - start, StandardCharsets.ISO_8859_1);
        String value = new String(content, nul + 1, Math.min(end - (nul + 1), 8192), StandardCharsets.ISO_8859_1);
        if (isGeneratorText(keyword, value)) {
            found.add(ProvenanceSignal.GENERATOR_METADATA);
        }
    }

    private static void inspectInternationalText(byte[] content, int start, int end, Set<ProvenanceSignal> found) {
        int nul = indexOfZero(content, start, end);
        if (nul < 0 || nul + 3 > end) {
            return;
        }
        String keyword = new String(content, start, nul - start, StandardCharsets.ISO_8859_1);
        int compressionFlag = content[nul + 1];
        int pos = nul + 3; // 압축 플래그, 압축 방식 다음부터 언어 태그
        int languageEnd = indexOfZero(content, pos, end);
        if (languageEnd < 0) {
            return;
        }
        int translatedEnd = indexOfZero(content, languageEnd + 1, end);
        if (translatedEnd < 0) {
            return;
        }
        String value = "";
        if (compressionFlag == 0) {
            int textStart = translatedEnd + 1;
            value = new String(content, textStart, Math.min(end - textStart, 8192), StandardCharsets.UTF_8);
        }
        if (isGeneratorText(keyword, value)) {
            found.add(ProvenanceSignal.GENERATOR_METADATA);
        }
    }

    /** 압축된 텍스트는 내용을 읽지 않고 키워드만 본다(InvokeAI처럼 키워드만으로 충분한 것). */
    private static void inspectKeywordOnly(byte[] content, int start, int end, Set<ProvenanceSignal> found) {
        int nul = indexOfZero(content, start, end);
        if (nul < 0) {
            return;
        }
        String keyword = new String(content, start, nul - start, StandardCharsets.ISO_8859_1);
        if (isGeneratorText(keyword, "")) {
            found.add(ProvenanceSignal.GENERATOR_METADATA);
        }
    }

    private static boolean isGeneratorText(String keyword, String value) {
        if (keyword.startsWith("invokeai") || keyword.equals("sd-metadata")) {
            return true;
        }
        if (keyword.equals("parameters")) {
            // Stable Diffusion WebUI(Automatic1111)가 남기는 형식: "..., Steps: 20, Sampler: ..."
            return value.contains("Steps:") || value.contains("Sampler:");
        }
        if (keyword.equals("prompt") || keyword.equals("workflow")) {
            // ComfyUI가 남기는 그래프 JSON
            return value.contains("class_type") || value.contains("\"nodes\"");
        }
        return false;
    }

    private static int indexOfZero(byte[] content, int from, int to) {
        for (int i = from; i < to; i++) {
            if (content[i] == 0) {
                return i;
            }
        }
        return -1;
    }

    private static long readUnsignedInt(byte[] content, int pos) {
        return ((content[pos] & 0xFFL) << 24)
                | ((content[pos + 1] & 0xFFL) << 16)
                | ((content[pos + 2] & 0xFFL) << 8)
                | (content[pos + 3] & 0xFFL);
    }
}
