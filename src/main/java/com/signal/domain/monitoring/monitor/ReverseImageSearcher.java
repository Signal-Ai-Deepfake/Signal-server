package com.signal.domain.monitoring.monitor;

import java.util.List;

public interface ReverseImageSearcher {

    /** 이미지와 같거나 비슷한 이미지가 실린 웹 페이지를 찾는다. 실패하면 예외를 던진다. */
    List<SearchHit> search(byte[] image);

    /**
     * @param pageUrl 이미지가 실린 페이지
     * @param imageUrl 페이지에 있는 일치 이미지(없으면 null)
     * @param similarity 0~1. 얼굴 동일인 판정이 아니라 이미지 일치 정도다(전체 일치 1.0, 부분 일치 0.8, 페이지만 0.6)
     */
    record SearchHit(String pageUrl, String imageUrl, String title, double similarity) {
    }
}
