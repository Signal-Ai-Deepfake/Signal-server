package com.signal.domain.chat.controller;

import com.signal.domain.chat.dto.request.CreateChatSessionRequest;
import com.signal.domain.chat.dto.request.SendMessageRequest;
import com.signal.domain.chat.dto.response.ChatMessagesResponse;
import com.signal.domain.chat.dto.response.ChatSessionCreateResponse;
import com.signal.domain.chat.dto.response.ChatSessionResponse;
import com.signal.domain.chat.dto.response.SendMessageResponse;
import com.signal.domain.chat.entity.ChatMessage;
import com.signal.domain.chat.entity.ChatSession;
import com.signal.domain.chat.service.ChatService;
import com.signal.domain.chat.service.SendMessageResult;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/sessions")
    public ResponseEntity<ChatSessionCreateResponse> createSession(
            @AuthenticationPrincipal Long userId,
            @RequestHeader(value = "X-Anonymous-Id", required = false) String anonymousId,
            @RequestBody(required = false) CreateChatSessionRequest request) {
        boolean saveConsent = request != null && request.saveConsentOrDefault();
        ChatSession session = chatService.createSession(userId, anonymousId, saveConsent);
        return ResponseEntity.status(HttpStatus.CREATED).body(ChatSessionCreateResponse.from(session));
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<ChatSessionResponse>> getMySessions(@AuthenticationPrincipal Long userId) {
        List<ChatSessionResponse> sessions = chatService.getMySessions(userId).stream()
                .map(ChatSessionResponse::from)
                .toList();
        return ResponseEntity.ok(sessions);
    }

    @PostMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<SendMessageResponse> sendMessage(
            @AuthenticationPrincipal Long userId,
            @RequestHeader(value = "X-Anonymous-Id", required = false) String anonymousId,
            @PathVariable String sessionId,
            @Valid @RequestBody SendMessageRequest request) {
        SendMessageResult result = chatService.sendMessage(sessionId, userId, anonymousId, request.content());
        return ResponseEntity.ok(SendMessageResponse.from(result));
    }

    @GetMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<ChatMessagesResponse> getMessages(
            @AuthenticationPrincipal Long userId,
            @RequestHeader(value = "X-Anonymous-Id", required = false) String anonymousId,
            @PathVariable String sessionId) {
        List<ChatMessage> messages = chatService.getMessages(sessionId, userId, anonymousId);
        return ResponseEntity.ok(ChatMessagesResponse.from(messages));
    }
}
