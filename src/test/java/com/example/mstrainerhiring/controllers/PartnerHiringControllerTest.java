package com.example.mstrainerhiring.controllers;

import com.example.mstrainerhiring.dto.PartnerHiringDTO;
import com.example.mstrainerhiring.enums.PartnerStatus;
import com.example.mstrainerhiring.services.PartnerHiringService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PartnerHiringControllerTest {

    @Mock
    private PartnerHiringService partnerHiringService;

    @InjectMocks
    private PartnerHiringController partnerHiringController;

    private PartnerHiringDTO partnerDTO;
    private final UUID partnerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        partnerDTO = new PartnerHiringDTO();
        partnerDTO.setId(partnerId);
        partnerDTO.setEmail("test@partner.com");
        partnerDTO.setOrganizationName("Test Org");
        partnerDTO.setStatus(PartnerStatus.PENDING);
    }

    @Test
    void shouldGetAllPartners() {
        // Arrange
        Page<PartnerHiringDTO> page = new PageImpl<>(List.of(partnerDTO));
        when(partnerHiringService.getAllPartners(any(Pageable.class), any())).thenReturn(page);

        // Act
        ResponseEntity<Page<PartnerHiringDTO>> response = partnerHiringController.getAllPartners(Pageable.unpaged(), null);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).hasSize(1);
        assertThat(response.getBody().getContent().get(0).getEmail()).isEqualTo("test@partner.com");

        verify(partnerHiringService, times(1)).getAllPartners(any(Pageable.class), any());
    }

    @Test
    void shouldGetPartnerById() {
        // Arrange
        when(partnerHiringService.getPartnerById(partnerId)).thenReturn(partnerDTO);

        // Act
        ResponseEntity<PartnerHiringDTO> response = partnerHiringController.getPartnerById(partnerId);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(partnerId);

        verify(partnerHiringService, times(1)).getPartnerById(partnerId);
    }

    @Test
    void shouldUpdatePartnerStatus() {
        // Arrange
        partnerDTO.setStatus(PartnerStatus.ACCEPTED);
        when(partnerHiringService.updateStatus(eq(partnerId), eq(PartnerStatus.ACCEPTED))).thenReturn(partnerDTO);

        // Act
        ResponseEntity<PartnerHiringDTO> response = partnerHiringController.updateStatus(partnerId, PartnerStatus.ACCEPTED);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(PartnerStatus.ACCEPTED);

        verify(partnerHiringService, times(1)).updateStatus(partnerId, PartnerStatus.ACCEPTED);
    }

    @Test
    void shouldDeletePartner() {
        // Arrange
        doNothing().when(partnerHiringService).deletePartner(partnerId);

        // Act
        ResponseEntity<Void> response = partnerHiringController.deletePartner(partnerId);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(partnerHiringService, times(1)).deletePartner(partnerId);
    }
}
