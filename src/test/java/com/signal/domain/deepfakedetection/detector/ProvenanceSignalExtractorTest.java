package com.signal.domain.deepfakedetection.detector;

import static org.assertj.core.api.Assertions.assertThat;

import com.signal.domain.deepfakedetection.entity.ProvenanceSignal;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProvenanceSignalExtractorTest {

    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};

    private static void chunk(ByteArrayOutputStream out, String type, byte[] data) {
        int length = data.length;
        out.write(length >>> 24);
        out.write(length >>> 16);
        out.write(length >>> 8);
        out.write(length);
        out.writeBytes(type.getBytes(StandardCharsets.ISO_8859_1));
        out.writeBytes(data);
        out.writeBytes(new byte[]{0, 0, 0, 0}); // CRC는 확인하지 않는다
    }

    private static byte[] text(String keyword, String value) {
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        data.writeBytes(keyword.getBytes(StandardCharsets.ISO_8859_1));
        data.write(0);
        data.writeBytes(value.getBytes(StandardCharsets.ISO_8859_1));
        return data.toByteArray();
    }

    private static byte[] internationalText(String keyword, String value) {
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        data.writeBytes(keyword.getBytes(StandardCharsets.ISO_8859_1));
        data.write(0);   // 키워드 끝
        data.write(0);   // 압축 안 함
        data.write(0);   // 압축 방식
        data.write(0);   // 언어 태그(빈 값) 끝
        data.write(0);   // 번역된 키워드(빈 값) 끝
        data.writeBytes(value.getBytes(StandardCharsets.UTF_8));
        return data.toByteArray();
    }

    private static byte[] pngWith(String chunkType, byte[] chunkData) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes(PNG_SIGNATURE);
        chunk(out, "IHDR", new byte[13]);
        chunk(out, chunkType, chunkData);
        chunk(out, "IDAT", new byte[32]);
        chunk(out, "IEND", new byte[0]);
        return out.toByteArray();
    }

    @Test
    void Stable_Diffusion_WebUI의_parameters_텍스트_청크를_찾는다() {
        byte[] file = pngWith("tEXt", text("parameters", "a cat\nNegative prompt: bad\nSteps: 20, Sampler: Euler a, Seed: 1"));

        assertThat(ProvenanceSignalExtractor.extract(file)).containsExactly(ProvenanceSignal.GENERATOR_METADATA);
    }

    @Test
    void ComfyUI의_prompt_그래프를_찾는다() {
        byte[] file = pngWith("tEXt", text("prompt", "{\"3\": {\"class_type\": \"KSampler\", \"inputs\": {}}}"));

        assertThat(ProvenanceSignalExtractor.extract(file)).containsExactly(ProvenanceSignal.GENERATOR_METADATA);
    }

    @Test
    void iTXt_청크의_parameters도_찾는다() {
        byte[] file = pngWith("iTXt", internationalText("parameters", "portrait, Steps: 30, Sampler: DPM++"));

        assertThat(ProvenanceSignalExtractor.extract(file)).containsExactly(ProvenanceSignal.GENERATOR_METADATA);
    }

    @Test
    void 평범한_PNG_텍스트는_신호가_아니다() {
        byte[] file = pngWith("tEXt", text("Comment", "여행 사진, Steps: 보행수"));
        byte[] genericParameters = pngWith("tEXt", text("parameters", "dpi=300"));
        byte[] genericPrompt = pngWith("tEXt", text("prompt", "프린터 설정"));

        assertThat(ProvenanceSignalExtractor.extract(file)).isEmpty();
        assertThat(ProvenanceSignalExtractor.extract(genericParameters)).isEmpty();
        assertThat(ProvenanceSignalExtractor.extract(genericPrompt)).isEmpty();
    }

    @Test
    void IPTC_digitalSourceType이_AI_생성이면_신호() {
        byte[] file = ("ÿØÿá padding padding "
                + "<Iptc4xmpExt:DigitalSourceType>http://cv.iptc.org/newscodes/digitalsourcetype/trainedAlgorithmicMedia"
                + "</Iptc4xmpExt:DigitalSourceType>").getBytes(StandardCharsets.ISO_8859_1);

        assertThat(ProvenanceSignalExtractor.extract(file)).containsExactly(ProvenanceSignal.AI_SOURCE_DECLARED);
    }

    @Test
    void 카메라_촬영을_뜻하는_digitalSourceType은_신호가_아니다() {
        byte[] file = ("padding padding padding padding "
                + "http://cv.iptc.org/newscodes/digitalsourcetype/digitalCapture").getBytes(StandardCharsets.ISO_8859_1);

        assertThat(ProvenanceSignalExtractor.extract(file)).isEmpty();
    }

    @Test
    void 알려진_생성_도구_이름이_메타데이터에_있으면_신호() {
        byte[] file = "padding padding <xmp:CreatorTool>Midjourney</xmp:CreatorTool> padding"
                .getBytes(StandardCharsets.ISO_8859_1);

        assertThat(ProvenanceSignalExtractor.extract(file)).containsExactly(ProvenanceSignal.GENERATOR_METADATA);
    }

    @Test
    void C2PA_매니페스트는_있다는_사실만_알리고_강한_신호가_아니다() {
        byte[] file = "padding padding jumb  jumd c2pa padding padding".getBytes(StandardCharsets.ISO_8859_1);

        List<ProvenanceSignal> signals = ProvenanceSignalExtractor.extract(file);

        assertThat(signals).containsExactly(ProvenanceSignal.C2PA_MANIFEST);
        assertThat(ProvenanceSignalExtractor.hasStrongSignal(signals)).isFalse();
    }

    @Test
    void 강한_신호가_하나라도_있으면_hasStrongSignal() {
        assertThat(ProvenanceSignalExtractor.hasStrongSignal(
                List.of(ProvenanceSignal.C2PA_MANIFEST, ProvenanceSignal.GENERATOR_METADATA))).isTrue();
        assertThat(ProvenanceSignalExtractor.hasStrongSignal(List.of())).isFalse();
        assertThat(ProvenanceSignalExtractor.hasStrongSignal(null)).isFalse();
    }

    @Test
    void 비었거나_이미지가_아니거나_깨진_입력은_예외_없이_빈_목록() {
        assertThat(ProvenanceSignalExtractor.extract(null)).isEmpty();
        assertThat(ProvenanceSignalExtractor.extract(new byte[0])).isEmpty();
        assertThat(ProvenanceSignalExtractor.extract(new byte[]{1, 2, 3})).isEmpty();
        byte[] truncated = pngWith("tEXt", text("parameters", "Steps: 20"));
        byte[] cut = java.util.Arrays.copyOf(truncated, 40); // 청크 길이가 파일 끝을 넘는다
        assertThat(ProvenanceSignalExtractor.extract(cut)).isEmpty();
    }

    @Test
    void 메타데이터가_없는_이미지는_신호가_없다() {
        byte[] file = new byte[4096];
        java.util.Arrays.fill(file, (byte) 0x55);

        assertThat(ProvenanceSignalExtractor.extract(file)).isEmpty();
    }
}
