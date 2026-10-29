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
 * - 파일 앞부분(EXIF, XMP, C2PA 매니페스트가 있는 영역)의 IPTC digitalSourceType 값과 알려진 생성 도구 이름
 * - C2PA(JUMBF) 매니페스트의 존재 여부(서명은 검증하지 않는다)
 *
 * 한계: 메타데이터는 스크린샷, 재저장, 메신저 전송으로 사라진다. 그래서 신호가 없다는 것은 아무 의미도 없다.
 * 신호가 있을 때만 근거로 쓴다. 이름 검색은 휴리스틱이라 오탐 가능성이 낮은 구체적인 문자열만 쓴다.
 */
public final class ProvenanceSignalExtractor {

    /** 이름 검색을 하는 파일 앞부분 크기. 메타데이터와 C2PA 매니페스트는 파일 앞쪽에 있다. */
    static final int SCAN_LIMIT = 2 * 1024 * 1024;

    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};

    private static final List<String> AI_SOURCE_VALUES = List.of(
            "trainedAlgorithmicMedia", "compositeWithTrainedAlgorithmicMedia", "algorithmicMedia", "compositeSynthetic");

    private static final List<String> GENERATOR_NAMES = List.of(
            "Midjourney", "DALL-E", "Stable Diffusion", "ComfyUI", "NovelAI", "Adobe Firefly", "Leonardo.Ai");

    private ProvenanceSignalExtractor() {
    }

    public static List<ProvenanceSignal> extract(byte[] content) {
        if (content == null || content.length < 16) {
            return List.of();
        }
        Set<ProvenanceSignal> found = EnumSet.noneOf(ProvenanceSignal.class);

        byte[] head = content.length > SCAN_LIMIT ? Arrays.copyOf(content, SCAN_LIMIT) : content;
        String text = new String(head, StandardCharsets.ISO_8859_1);
        for (String value : AI_SOURCE_VALUES) {
            if (text.contains(value)) {
                found.add(ProvenanceSignal.AI_SOURCE_DECLARED);
                break;
            }
        }
        for (String name : GENERATOR_NAMES) {
            if (text.contains(name)) {
                found.add(ProvenanceSignal.GENERATOR_METADATA);
                break;
            }
        }
        if (text.contains("jumb") && text.contains("c2pa")) {
            found.add(ProvenanceSignal.C2PA_MANIFEST);
        }

        if (isPng(content)) {
            scanPngTextChunks(content, found);
        }
        return new ArrayList<>(found);
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
