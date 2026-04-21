package tn.esprit.microservice.reclamation.services.impl;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.microservice.reclamation.entities.Reclamation;
import tn.esprit.microservice.reclamation.entities.ReclamationResponse;
import tn.esprit.microservice.reclamation.entities.ReclamationStatus;
import tn.esprit.microservice.reclamation.repositories.ReclamationRepository;
import tn.esprit.microservice.reclamation.repositories.ReclamationResponseRepository;

import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReclamationResponseServiceImplTest {

    @Mock
    private ReclamationResponseRepository responseRepository;

    @Mock
    private ReclamationRepository reclamationRepository;

    @InjectMocks
    private ReclamationResponseServiceImpl responseService;

    private Reclamation reclamation;

    private ReclamationResponse response;

    @BeforeEach
    void setUp() {
        reclamation = Reclamation.builder()
                .id(1L)
                .status(ReclamationStatus.PENDING)
                .build();

        response = ReclamationResponse.builder()
                .id(1L)
                .reclamationId(1L)
                .responseText("Test response")
                .isInternal(false)
                .build();
    }

    // ================= CREATE RESPONSE =================
    @Test
    void testCreateResponse_success() {

        when(reclamationRepository.findById(1L))
                .thenReturn(Optional.of(reclamation));

        when(responseRepository.save(any()))
                .thenAnswer(i -> i.getArgument(0));

        ReclamationResponse result = responseService.createResponse(
                1L,
                "100",
                "Hello",
                false
        );

        assertNotNull(result);
        assertEquals("Hello", result.getResponseText());
        assertEquals("100", result.getSenderId());
        assertFalse(result.getIsInternal());
    }

    // ================= GET BY ID =================
    @Test
    void testGetResponseById() {

        when(responseRepository.findById(1L))
                .thenReturn(Optional.of(response));

        ReclamationResponse result = responseService.getResponseById(1L);

        assertEquals("Test response", result.getResponseText());
    }

    // ================= GET ALL =================
    @Test
    void testGetResponsesByReclamationId() {

        when(responseRepository.findByReclamationId(1L))
                .thenReturn(List.of(response));

        List<ReclamationResponse> result = responseService.getResponsesByReclamationId(1L);

        assertEquals(1, result.size());
    }

    // ================= DELETE =================
    @Test
    void testDeleteResponse() {

        when(responseRepository.findById(1L))
                .thenReturn(Optional.of(response));

        responseService.deleteResponse(1L);

        verify(responseRepository, times(1)).delete(response);
    }
}