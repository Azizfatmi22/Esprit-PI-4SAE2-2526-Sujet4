package com.formini.msliveclass.controllers;

import com.formini.msliveclass.dto.ErrorResponse;
import com.formini.msliveclass.entities.ChatMessage;
import com.formini.msliveclass.entities.LiveSession;
import com.formini.msliveclass.repositories.ChatMessageRepository;
import com.formini.msliveclass.repositories.LiveSessionRepository;
import com.formini.msliveclass.services.SessionAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private LiveSessionRepository liveSessionRepository;

    @Mock
    private SessionAccessService sessionAccessService;

    @InjectMocks
    private ChatController chatController;

    private LiveSession activeSession;
    private ChatMessage chatMessage;

    @BeforeEach
    void setUp() {
        activeSession = new LiveSession();
        activeSession.setId(1L);
        activeSession.setCourseId(10L);
        activeSession.setIsActive(true);
        activeSession.setChatEnabled(true);

        chatMessage = new ChatMessage();
        chatMessage.setSessionId(1L);
        chatMessage.setSenderId("learner-1");
        chatMessage.setContent("Hello everyone!");
    }

    @Test
    void sendMessage_ValidationFailures() {
        // Session ID missing
        chatMessage.setSessionId(null);
        ResponseEntity<Object> response = chatController.sendMessage(chatMessage);
        assertEquals(400, response.getStatusCode().value());
        assertTrue(response.getBody() instanceof ErrorResponse);

        // Sender ID missing
        chatMessage.setSessionId(1L);
        chatMessage.setSenderId("");
        response = chatController.sendMessage(chatMessage);
        assertEquals(400, response.getStatusCode().value());

        // Content missing
        chatMessage.setSenderId("learner-1");
        chatMessage.setContent("  ");
        response = chatController.sendMessage(chatMessage);
        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void sendMessage_SessionNotFound() {
        when(liveSessionRepository.findById(1L)).thenReturn(Optional.empty());

        ResponseEntity<Object> response = chatController.sendMessage(chatMessage);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Session not found", ((ErrorResponse) response.getBody()).getMessage());
    }

    @Test
    void sendMessage_SessionNotActive() {
        activeSession.setIsActive(false);
        when(liveSessionRepository.findById(1L)).thenReturn(Optional.of(activeSession));

        ResponseEntity<Object> response = chatController.sendMessage(chatMessage);

        assertEquals(403, response.getStatusCode().value());
        assertEquals("This live session is not active", ((ErrorResponse) response.getBody()).getMessage());
    }

    @Test
    void sendMessage_ChatDisabled() {
        activeSession.setChatEnabled(false);
        when(liveSessionRepository.findById(1L)).thenReturn(Optional.of(activeSession));

        ResponseEntity<Object> response = chatController.sendMessage(chatMessage);

        assertEquals(403, response.getStatusCode().value());
        assertEquals("Chat is disabled for this session", ((ErrorResponse) response.getBody()).getMessage());
    }

    @Test
    void sendMessage_AccessDenied() {
        when(liveSessionRepository.findById(1L)).thenReturn(Optional.of(activeSession));
        when(sessionAccessService.isCourseTrainer(10L, "learner-1")).thenReturn(false);
        when(sessionAccessService.hasPaidEnrollment(10L, "learner-1")).thenReturn(false);

        ResponseEntity<Object> response = chatController.sendMessage(chatMessage);

        assertEquals(403, response.getStatusCode().value());
        assertTrue(((ErrorResponse) response.getBody()).getMessage().contains("Only the trainer and learners who paid"));
    }

    @Test
    void sendMessage_Success_Trainer() {
        when(liveSessionRepository.findById(1L)).thenReturn(Optional.of(activeSession));
        when(sessionAccessService.isCourseTrainer(10L, "learner-1")).thenReturn(true);
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(chatMessage);

        ResponseEntity<Object> response = chatController.sendMessage(chatMessage);

        assertEquals(200, response.getStatusCode().value());
        verify(chatMessageRepository).save(any(ChatMessage.class));
    }

    @Test
    void sendMessage_Success_Learner() {
        when(liveSessionRepository.findById(1L)).thenReturn(Optional.of(activeSession));
        when(sessionAccessService.isCourseTrainer(10L, "learner-1")).thenReturn(false);
        when(sessionAccessService.hasPaidEnrollment(10L, "learner-1")).thenReturn(true);
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(chatMessage);

        ResponseEntity<Object> response = chatController.sendMessage(chatMessage);

        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void sendMessage_SpamProtection_Throttle() {
        when(liveSessionRepository.findById(1L)).thenReturn(Optional.of(activeSession));
        when(sessionAccessService.isCourseTrainer(10L, "learner-1")).thenReturn(false);
        when(sessionAccessService.hasPaidEnrollment(10L, "learner-1")).thenReturn(true);
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(chatMessage);

        // First message
        chatController.sendMessage(chatMessage);
        
        // Immediate second message (within 2s throttle)
        ResponseEntity<Object> response = chatController.sendMessage(chatMessage);

        assertEquals(429, response.getStatusCode().value());
        assertTrue(((ErrorResponse) response.getBody()).getMessage().contains("Please wait"));
    }

    @Test
    void getMessages_SessionNotFound() {
        when(liveSessionRepository.findById(1L)).thenReturn(Optional.empty());

        ResponseEntity<Object> response = chatController.getMessages(1L, "user-1");

        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void getMessages_Success() {
        when(liveSessionRepository.findById(1L)).thenReturn(Optional.of(activeSession));
        when(chatMessageRepository.findBySessionId(1L)).thenReturn(Arrays.asList(chatMessage));

        ResponseEntity<Object> response = chatController.getMessages(1L, null);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, ((List<?>) response.getBody()).size());
    }

    @Test
    void getAllMessages() {
        when(chatMessageRepository.findAll()).thenReturn(Arrays.asList(chatMessage));
        List<ChatMessage> result = chatController.getAllMessages();
        assertEquals(1, result.size());
    }

    @Test
    void deleteMessage_NotFound() {
        when(chatMessageRepository.findById(1L)).thenReturn(Optional.empty());

        ResponseEntity<Object> response = chatController.deleteMessage(1L, "trainer-1");

        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    void deleteMessage_NotTrainer() {
        ChatMessage msg = new ChatMessage();
        msg.setSessionId(1L);
        when(chatMessageRepository.findById(1L)).thenReturn(Optional.of(msg));
        when(liveSessionRepository.findById(1L)).thenReturn(Optional.of(activeSession));
        when(sessionAccessService.isCourseTrainer(10L, "not-trainer")).thenReturn(false);

        ResponseEntity<Object> response = chatController.deleteMessage(1L, "not-trainer");

        assertEquals(403, response.getStatusCode().value());
    }

    @Test
    void deleteMessage_Success() {
        ChatMessage msg = new ChatMessage();
        msg.setSessionId(1L);
        when(chatMessageRepository.findById(1L)).thenReturn(Optional.of(msg));
        when(liveSessionRepository.findById(1L)).thenReturn(Optional.of(activeSession));
        when(sessionAccessService.isCourseTrainer(10L, "trainer-1")).thenReturn(true);

        ResponseEntity<Object> response = chatController.deleteMessage(1L, "trainer-1");

        assertEquals(200, response.getStatusCode().value());
        verify(chatMessageRepository).delete(msg);
    }
}
