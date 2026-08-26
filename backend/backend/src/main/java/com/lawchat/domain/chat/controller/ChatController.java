package com.lawchat.domain.chat.controller;

import com.lawchat.domain.chat.dto.request.ChatMessageRequest;
import com.lawchat.domain.chat.dto.request.FeedbackRequest;
import com.lawchat.domain.chat.dto.response.ChatMessageResponse;
import com.lawchat.domain.chat.dto.response.ChatSessionResponse;
import com.lawchat.domain.chat.service.ChatFeedbackService;
import com.lawchat.domain.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * /api/chat/*
 * 프론트 매칭:
 *  - ChatSidebar, ChatHistoryList -> GET /sessions
 *  - FavoritesList                -> GET /sessions/favorites
 *  - NewChatInput                 -> POST /sessions, POST /sessions/{id}/messages
 *  - MessageBubble                -> GET /sessions/{id}/messages
 */

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final ChatFeedbackService chatFeedbackService;

    @PostMapping("/sessions")
    public ResponseEntity<ChatSessionResponse> createSession(@AuthenticationPrincipal Long userId,
                                                               @RequestParam(defaultValue = "새 대화") String title) {
        return ResponseEntity.ok(chatService.createSession(userId, title));
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<ChatSessionResponse>> getSessions(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(chatService.getSessions(userId));
    }

    @GetMapping("/sessions/favorites")
    public ResponseEntity<List<ChatSessionResponse>> getFavoriteSessions(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(chatService.getFavoriteSessions(userId));
    }

    @PatchMapping("/sessions/{sessionId}")
    public ResponseEntity<ChatSessionResponse> renameSession(@AuthenticationPrincipal Long userId,
                                                               @PathVariable Long sessionId,
                                                               @RequestParam String title) {
        return ResponseEntity.ok(chatService.renameSession(userId, sessionId, title));
    }

    @PatchMapping("/sessions/{sessionId}/favorite")
    public ResponseEntity<ChatSessionResponse> toggleFavorite(@AuthenticationPrincipal Long userId,
                                                                @PathVariable Long sessionId) {
        return ResponseEntity.ok(chatService.toggleFavorite(userId, sessionId));
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Void> deleteSession(@AuthenticationPrincipal Long userId,
                                               @PathVariable Long sessionId) {
        chatService.deleteSession(userId, sessionId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<List<ChatMessageResponse>> getMessages(@AuthenticationPrincipal Long userId,
                                                                   @PathVariable Long sessionId) {
        return ResponseEntity.ok(chatService.getMessages(userId, sessionId));
    }

    @PostMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<ChatMessageResponse> sendMessage(@AuthenticationPrincipal Long userId,
                                                             @PathVariable Long sessionId,
                                                             @RequestBody ChatMessageRequest request) {
        return ResponseEntity.ok(chatService.sendMessage(userId, sessionId, request));
    }

    // FeedbackModal
    @PostMapping("/messages/{messageId}/feedback")
    public ResponseEntity<Void> submitFeedback(@PathVariable Long messageId,
                                                @RequestBody FeedbackRequest request) {
        chatFeedbackService.submitFeedback(messageId, request);
        return ResponseEntity.noContent().build();
    }
}
