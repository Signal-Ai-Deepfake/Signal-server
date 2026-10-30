package com.signal.domain.websearch.service;

import com.signal.domain.monitoring.monitor.ReverseImageSearcher;
import com.signal.domain.websearch.entity.UnavailableReason;
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
 * 외부 서비스의 무료 한도(SerpApi 월 250건)를 지키려고 사용자별 하루, 전체 하루, 전체 한 달 상한을 두며,
 * 상한에 닿거나 영상이면 UNAVAILABLE과 이유를 기록한다.
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
    private final int monthlyLimit;
    private final int ownerDailyLimit;

    public WebSearchService(WebSearchRepository webSearchRepository,
                            WebSearchRunner runner,
                            @Qualifier("serpApi") ObjectProvider<ReverseImageSearcher> searcher,
                            @Value("${web-search.daily-limit:8}") int dailyLimit,
                            @Value("${web-search.monthly-limit:240}") int monthlyLimit,
                            @Value("${web-search.owner-daily-limit:2}") int ownerDailyLimit) {
        this.webSearchRepository = webSearchRepository;
        this.runner = runner;
        this.searcher = searcher.getIfAvailable();
        this.dailyLimit = dailyLimit;
        this.monthlyLimit = monthlyLimit;
        this.ownerDailyLimit = ownerDailyLimit;
    }

    public void start(Long detectionId, String fileUrl, boolean isVideo, String ownerKey) {
        if (searcher == null) {
            return;
        }
        try {
            UnavailableReason reason = unavailableReason(isVideo, ownerKey);
            if (reason != null) {
                log.warn("웹 검색을 하지 않습니다: detectionId={}, 이유={}", detectionId, reason);
                webSearchRepository.save(WebSearch.unavailable(detectionId, ownerKey, reason));
                return;
            }
            webSearchRepository.save(WebSearch.processing(detectionId, ownerKey));
            runner.search(detectionId, fileUrl, searcher);
        } catch (Exception e) {
            // 웹 검색 준비가 실패해도 탐지 요청 자체는 성공해야 한다.
            log.error("웹 검색 시작 실패: detectionId={}", detectionId, e);
        }
    }

    /** 영상이거나 한도(사용자별 하루, 전체 하루, 전체 한 달)에 닿았으면 이유를, 검색해도 되면 null을 돌려준다. */
    private UnavailableReason unavailableReason(boolean isVideo, String ownerKey) {
        if (isVideo) {
            return UnavailableReason.VIDEO;
        }
        LocalDateTime today = startOfTodayKst();
        if (ownerKey != null && webSearchRepository.countByOwnerKeyAndCreatedAtGreaterThanEqualAndStatusNot(
                ownerKey, today, WebSearchStatus.UNAVAILABLE) >= ownerDailyLimit) {
            return UnavailableReason.OWNER_LIMIT;
        }
        if (webSearchRepository.countByCreatedAtGreaterThanEqualAndStatusNot(
                today, WebSearchStatus.UNAVAILABLE) >= dailyLimit) {
            return UnavailableReason.DAILY_LIMIT;
        }
        if (webSearchRepository.countByCreatedAtGreaterThanEqualAndStatusNot(
                startOfMonthKst(), WebSearchStatus.UNAVAILABLE) >= monthlyLimit) {
            return UnavailableReason.MONTHLY_LIMIT;
        }
        return null;
    }

    public WebSearch find(Long detectionId) {
        Optional<WebSearch> found = webSearchRepository.findByDetectionId(detectionId);
        return found.orElse(null);
    }

    /** 저장된 시각은 서버 기본 시간대이므로, 한국 시간 자정을 같은 시간대로 옮겨 비교한다. */
    private static LocalDateTime startOfTodayKst() {
        return LocalDate.now(KST).atStartOfDay(KST).withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
    }

    private static LocalDateTime startOfMonthKst() {
        return LocalDate.now(KST).withDayOfMonth(1).atStartOfDay(KST)
                .withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
    }
}
