package com.formini.msliveclass.controllers;

import com.formini.msliveclass.clients.CourseClient;
import com.formini.msliveclass.dto.CourseDTO;
import com.formini.msliveclass.dto.LiveSessionResponse;
import com.formini.msliveclass.entities.ChatMessage;
import com.formini.msliveclass.entities.LiveSession;
import com.formini.msliveclass.entities.Poll;
import com.formini.msliveclass.repositories.ChatMessageRepository;
import com.formini.msliveclass.repositories.LiveSessionRepository;
import com.formini.msliveclass.repositories.PollRepository;
import com.formini.msliveclass.repositories.PollVoteRepository;
import com.formini.msliveclass.services.SessionAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LiveSessionControllerTest {

    @Mock private LiveSessionRepository liveSessionRepository;
    @Mock private CourseClient courseClient;
    @Mock private SessionAccessService sessionAccessService;
    @Mock private PollRepository pollRepository;
    @Mock private PollVoteRepository pollVoteRepository;
    @Mock private ChatMessageRepository chatMessageRepository;

    @InjectMocks
    private LiveSessionController liveSessionController;

    private LiveSession testSession;
    private String trainerId = "trainer-1";

    @BeforeEach
    void setUp() {
        testSession = new LiveSession();
        testSession.setId(1L);
        testSession.setCourseId(10L);
        testSession.setIsActive(true);
        testSession.setMeetingLink("https://meet.jit.si/test");
        testSession.setStartedAt(LocalDateTime.now().minusHours(1));
    }

    @Test
    void createSession_Success() {
        when(sessionAccessService.isCourseTrainer(10L, trainerId)).thenReturn(true);
        when(liveSessionRepository.findAll()).thenReturn(Collections.emptyList());
        when(liveSessionRepository.findByCourseId(10L)).thenReturn(Collections.emptyList());
        when(liveSessionRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        ResponseEntity<?> response = liveSessionController.createSession(testSession, trainerId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(liveSessionRepository).save(any());
        verify(chatMessageRepository).save(any());
    }

    @Test
    void createSession_Unauthorized_ReturnsForbidden() {
        when(sessionAccessService.isCourseTrainer(10L, trainerId)).thenReturn(false);

        ResponseEntity<?> response = liveSessionController.createSession(testSession, trainerId);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void createSession_AlreadyHasActiveSession_ReturnsConflict() {
        when(sessionAccessService.isCourseTrainer(10L, trainerId)).thenReturn(true);
        LiveSession active = new LiveSession();
        active.setIsActive(true);
        active.setCourseId(11L);
        when(liveSessionRepository.findAll()).thenReturn(List.of(active));
        
        CourseDTO course = new CourseDTO();
        course.setTrainerId(trainerId);
        when(courseClient.getCourseById(11L)).thenReturn(course);

        ResponseEntity<?> response = liveSessionController.createSession(testSession, trainerId);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    void createSessionSimple_Success() {
        when(sessionAccessService.isCourseTrainer(10L, trainerId)).thenReturn(true);
        when(liveSessionRepository.findAll()).thenReturn(Collections.emptyList());
        when(liveSessionRepository.findByCourseId(10L)).thenReturn(Collections.emptyList());
        when(liveSessionRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        ResponseEntity<?> response = liveSessionController.createSessionSimple(testSession, trainerId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getSession_Success() {
        when(liveSessionRepository.findById(1L)).thenReturn(Optional.of(testSession));
        when(courseClient.getCourseTitle(10L)).thenReturn("Java Class");

        ResponseEntity<LiveSessionResponse> response = liveSessionController.getSession(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Java Class", response.getBody().getCourseTitle());
    }

    @Test
    void joinSession_Success() {
        when(liveSessionRepository.findById(1L)).thenReturn(Optional.of(testSession));
        when(sessionAccessService.hasPaidEnrollment(10L, "learner-1")).thenReturn(true);
        when(courseClient.getCourseTitle(10L)).thenReturn("Java Class");

        ResponseEntity<?> response = liveSessionController.joinSession(1L, "learner-1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void joinSession_NotPaid_ReturnsForbidden() {
        when(liveSessionRepository.findById(1L)).thenReturn(Optional.of(testSession));
        when(sessionAccessService.hasPaidEnrollment(10L, "learner-1")).thenReturn(false);

        ResponseEntity<?> response = liveSessionController.joinSession(1L, "learner-1");

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void getTotalLiveTime_Success() {
        LiveSession session1 = new LiveSession();
        session1.setStartedAt(LocalDateTime.now().minusHours(1));
        session1.setEndedAt(LocalDateTime.now()); // 60 mins -> counted
        
        LiveSession session2 = new LiveSession();
        session2.setStartedAt(LocalDateTime.now().minusMinutes(10));
        session2.setEndedAt(LocalDateTime.now()); // 10 mins -> skipped

        when(liveSessionRepository.findByCourseId(10L)).thenReturn(List.of(session1, session2));

        ResponseEntity<Map<String, Object>> response = liveSessionController.getTotalLiveTime(10L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(3600L, response.getBody().get("totalSeconds"));
        assertEquals(1, response.getBody().get("validSessionCount"));
    }

    @Test
    void endSession_SuccessWithPolls() {
        when(liveSessionRepository.findById(1L)).thenReturn(Optional.of(testSession));
        when(sessionAccessService.isCourseTrainer(10L, trainerId)).thenReturn(true);
        
        Poll poll = new Poll();
        poll.setId(100L);
        poll.setSessionId(1L);
        poll.setOptions(List.of("A", "B"));
        when(pollRepository.findBySessionIdAndIsActive(1L, true)).thenReturn(List.of(poll));
        when(pollVoteRepository.countByPollIdAndOptionIndex(100L, 0)).thenReturn(5L);
        when(pollVoteRepository.countByPollIdAndOptionIndex(100L, 1)).thenReturn(2L);

        ResponseEntity<?> response = liveSessionController.endSession(1L, trainerId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertFalse(testSession.getIsActive());
        assertNotNull(testSession.getEndedAt());
        verify(chatMessageRepository, atLeastOnce()).save(any());
    }

    @Test
    void toggleChat_Success() {
        when(liveSessionRepository.findById(1L)).thenReturn(Optional.of(testSession));
        when(sessionAccessService.isCourseTrainer(10L, trainerId)).thenReturn(true);
        when(liveSessionRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        testSession.setChatEnabled(true);
        ResponseEntity<?> response = liveSessionController.toggleChat(1L, trainerId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertFalse(((LiveSession)response.getBody()).getChatEnabled());
    }

    @Test
    void getAllSessions_Success() {
        when(liveSessionRepository.findAll()).thenReturn(List.of(testSession));
        when(courseClient.getCourseTitle(10L)).thenReturn("Title");

        List<LiveSessionResponse> result = liveSessionController.getAllSessions();
        assertEquals(1, result.size());
    }

    @Test
    void getSessionsByUser_Success() {
        when(liveSessionRepository.findAll()).thenReturn(List.of(testSession));
        CourseDTO course = new CourseDTO();
        course.setTrainerId(trainerId);
        when(courseClient.getCourseById(10L)).thenReturn(course);
        when(courseClient.getCourseTitle(10L)).thenReturn("Title");

        List<LiveSessionResponse> result = liveSessionController.getSessionsByUser(trainerId);
        assertEquals(1, result.size());
    }
}
