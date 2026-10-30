package com.signal.domain.deepfakedetection.dto.response;

import com.signal.domain.monitoring.entity.MatchType;
import com.signal.domain.websearch.entity.WebSearch;
import com.signal.domain.websearch.entity.WebSearchStatus;
import java.net.URI;
import java.util.List;

/**
 * 이 사진이 웹 다른 곳에 올라와 있는지 역이미지 검색한 결과. status가 COMPLETED일 때만 matches가 의미 있다.
 * 이미지 단위 일치를 찾은 것이라 "같은 사람의 다른 사진"은 찾지 못하며, 찾았다고 해서 도용이라는 뜻도 아니다
 * (본인이 올린 게시물일 수 있다). 점수가 아니므로 퍼센트로 표시하지 않는다.
 * matchType: FULL 동일 이미지 / SIMILAR 비슷한 이미지.
 */
public record WebSearchResponse(
        WebSearchStatus status,
        int matchCount,
        List<Match> matches
) {

    public record Match(String pageUrl, String domain, String title, MatchType matchType) {
    }

    public static WebSearchResponse from(WebSearch webSearch) {
        List<Match> matches = webSearch.getMatches().stream()
                .map(m -> new Match(m.getPageUrl(), domainOf(m.getPageUrl()), m.getTitle(), m.getMatchType()))
                .toList();
        return new WebSearchResponse(webSearch.getStatus(), matches.size(), matches);
    }

    private static String domainOf(String url) {
        try {
            return URI.create(url).getHost();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
