package com.signal.domain.chat.controller;

import com.signal.domain.chat.dto.request.SendMessageRequest;
import com.signal.domain.chat.dto.response.ChatMessagesResponse;
import com.signal.domain.chat.dto.response.ChatSessionCreateResponse;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/sessions")
    public ResponseEntity<ChatSessionCreateResponse> createSession() {
        ChatSession session = chatService.createSession();
        return ResponseEntity.status(HttpStatus.CREATED).body(ChatSessionCreateResponse.from(session));
    }

    @PostMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<SendMessageResponse> sendMessage(
            @PathVariable String sessionId,
            @Valid @RequestBody SendMessageRequest request) {
        SendMessageResult result = chatService.sendMessage(sessionId, request.content());
        return ResponseEntity.ok(SendMessageResponse.from(result));
    }

    @GetMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<ChatMessagesResponse> getMessages(@PathVariable String sessionId) {
        List<ChatMessage> messages = chatService.getMessages(sessionId);
        return ResponseEntity.ok(ChatMessagesResponse.from(messages));
    }
}
