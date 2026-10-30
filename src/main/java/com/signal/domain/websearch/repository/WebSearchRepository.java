package com.signal.domain.websearch.repository;

import com.signal.domain.websearch.entity.WebSearch;
import com.signal.domain.websearch.entity.WebSearchStatus;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WebSearchRepository extends JpaRepository<WebSearch, Long> {

    Optional<WebSearch> findByDetectionId(Long detectionId);

    long countByOwnerKeyAndCreatedAtGreaterThanEqualAndStatusNot(String ownerKey, LocalDateTime since, WebSearchStatus status);

    long countByCreatedAtGreaterThanEqualAndStatusNot(LocalDateTime since, WebSearchStatus status);
}
