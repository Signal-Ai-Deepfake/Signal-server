package com.signal.domain.chat.entity;

import com.signal.domain.chat.engine.SituationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "chat_sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatSession {

    private static final int PREVIEW_MAX_LENGTH = 100;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String sessionId;

    private Long userId;

    private String anonymousId;

    @Column(nullable = false)
    private boolean saveConsent;

    @Column(length = 100)
    private String firstMessagePreview;

    @Enumerated(EnumType.STRING)
    private SituationType lastSituationType;

    @Column(nullable = false)
    private boolean lastCrisisDetected;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Builder
    public ChatSession(String sessionId, Long userId, String anonymousId, boolean saveConsent) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.anonymousId = anonymousId;
        this.saveConsent = saveConsent;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public boolean isOwnedBy(Long userId, String anonymousId) {
        if (this.userId != null) {
            return this.userId.equals(userId);
        }
        return Objects.equals(this.anonymousId, anonymousId);
    }

    public void recordUserMessage(String content) {
        if (this.firstMessagePreview == null) {
            this.firstMessagePreview = content.length() > PREVIEW_MAX_LENGTH
                    ? content.substring(0, PREVIEW_MAX_LENGTH)
                    : content;
        }
        this.updatedAt = LocalDateTime.now();
    }

    public void recordEngineResult(SituationType situationType, boolean crisisDetected) {
        this.lastSituationType = situationType;
        this.lastCrisisDetected = crisisDetected;
    }
}
