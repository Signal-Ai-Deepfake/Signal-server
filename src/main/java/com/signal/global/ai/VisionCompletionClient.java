package com.signal.global.ai;

/**
 * 이미지를 입력으로 받는 비전(vision) LLM을 호출하는 클라이언트.
 * 딥페이크 탐지, 위험도 분석 등 이미지 분석이 필요한 여러 도메인에서 공용으로 사용한다.
 */
public interface VisionCompletionClient {

    /**
     * 이미지와 프롬프트를 비전 LLM에 보내고, 모델이 생성한 텍스트를 그대로 반환한다.
     * 호출부는 systemPrompt에서 순수 JSON으로만 응답하도록 지시하고, 반환된 텍스트를 직접 파싱한다.
     *
     * @param imageBytes   이미지 원본 바이트
     * @param mimeType     이미지 MIME 타입 (예: image/jpeg, image/png)
     * @param systemPrompt 역할·출력 형식을 지시하는 시스템 프롬프트
     * @param userPrompt   실제 분석 요청 문구 (이미지와 함께 전달됨)
     * @return LLM이 생성한 원문 텍스트 (보통 JSON)
     * @throws RuntimeException API 키 미설정, 네트워크 오류, 타임아웃, 빈 응답 등 실패 시 (호출부에서 폴백 처리)
     */
    String complete(byte[] imageBytes, String mimeType, String systemPrompt, String userPrompt);
}
