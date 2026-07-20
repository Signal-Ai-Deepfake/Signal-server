package com.signal.domain.report.entity;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Embeddable;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TimelineEntry {

    @Enumerated(EnumType.STRING)
    private TimelineEvent event;

    private LocalDateTime occurredAt;

    @Builder
    public TimelineEntry(TimelineEvent event, LocalDateTime occurredAt) {
        this.event = event;
        this.occurredAt = occurredAt;
    }
}
