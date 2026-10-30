package com.signal.domain.websearch.service;

import com.signal.domain.monitoring.monitor.ReverseImageSearcher;
import com.signal.domain.websearch.entity.WebSearch;
import com.signal.domain.websearch.entity.WebSearchStatus;
import com.signal.domain.websearch.repository.WebSearchRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 탐지한 사진을 웹에서 역검색한다. web-search.provider가 none(기본)이면 아무것도 하지 않고, 응답의 webSearch는 null이다.
 * 외부 서비스의 무료 한도를 지키려고 하루 전체 검색 횟수에 상한을 두며, 상한에 닿거나 영상이면 UNAVAILABLE로 기록한다.
 */
@Slf4j
@Service
public class WebSearchService {

    /** 일일 상한은 한국 시간 자정에 초기화한다(컨테이너가 UTC여도 같다). */
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final WebSearchRepository webSearchRepository;
    private final WebSearchRunner runner;
    private final ReverseImageSearcher searcher;   // null이면 기능이 꺼진 상태
    private final int dailyLimit;

    public WebSearchService(WebSearchRepository webSearchRepository,
                            WebSearchRunner runner,
                            @Qualifier("serpApi") ObjectProvider<ReverseImageSearcher> searcher,
                            @Value("${web-search.daily-limit:3}") int dailyLimit) {
        this.webSearchRepository = webSearchRepository;
        this.runner = runner;
        this.searcher = searcher.getIfAvailable();
        this.dailyLimit = dailyLimit;
    }

    public void start(Long detectionId, String fileUrl, boolean isVideo) {
        if (searcher == null) {
            return;
        }
        try {
            if (isVideo) {
                webSearchRepository.save(WebSearch.unavailable(detectionId));
                return;
            }
            if (webSearchRepository.countByCreatedAtGreaterThanEqualAndStatusNot(
                    startOfTodayKst(), WebSearchStatus.UNAVAILABLE) >= dailyLimit) {
                log.warn("오늘 웹 검색 상한({}건)에 도달해 검색하지 않습니다. detectionId={}", dailyLimit, detectionId);
                webSearchRepository.save(WebSearch.unavailable(detectionId));
                return;
            }
            webSearchRepository.save(WebSearch.processing(detectionId));
            runner.search(detectionId, fileUrl, searcher);
        } catch (Exception e) {
            // 웹 검색 준비가 실패해도 탐지 요청 자체는 성공해야 한다.
            log.error("웹 검색 시작 실패: detectionId={}", detectionId, e);
        }
    }

    public WebSearch find(Long detectionId) {
        Optional<WebSearch> found = webSearchRepository.findByDetectionId(detectionId);
        return found.orElse(null);
    }

    /** 저장된 시각은 서버 기본 시간대이므로, 한국 시간 자정을 같은 시간대로 옮겨 비교한다. */
    private static LocalDateTime startOfTodayKst() {
        return LocalDate.now(KST).atStartOfDay(KST).withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
    }
}
