package com.signal.domain.chat.repository;

import com.signal.domain.chat.entity.ChatSession;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

    Optional<ChatSession> findBySessionId(String sessionId);
}
