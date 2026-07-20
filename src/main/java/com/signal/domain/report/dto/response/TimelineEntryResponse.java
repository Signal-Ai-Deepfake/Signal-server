package com.signal.domain.report.dto.response;

import com.signal.domain.report.entity.TimelineEntry;
import com.signal.domain.report.entity.TimelineEvent;
import java.time.LocalDateTime;

public record TimelineEntryResponse(TimelineEvent event, LocalDateTime occurredAt) {

    public static TimelineEntryResponse from(TimelineEntry entry) {
        return new TimelineEntryResponse(entry.getEvent(), entry.getOccurredAt());
    }
}
