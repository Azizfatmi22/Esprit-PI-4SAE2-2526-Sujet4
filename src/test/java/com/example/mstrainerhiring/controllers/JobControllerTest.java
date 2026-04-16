package com.example.mstrainerhiring.controllers;

import com.example.mstrainerhiring.dto.JobDTO;
import com.example.mstrainerhiring.entities.Job;
import com.example.mstrainerhiring.enums.City;
import com.example.mstrainerhiring.enums.Technology;
import com.example.mstrainerhiring.mapper.JobMapper;
import com.example.mstrainerhiring.services.JobService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class JobControllerTest {

    @Mock
    private JobService jobService;

    @Mock
    private JobMapper jobMapper;

    @InjectMocks
    private JobController jobController;

    private Job job;
    private JobDTO jobDTO;
    private final UUID partnerId = UUID.randomUUID();
    private final UUID jobId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        job = Job.builder()
                .id(jobId)
                .title("Software Engineer")
                .technology(Technology.JAVA)
                .minExperience(3)
                .location(City.TUNIS)
                .build();

        jobDTO = new JobDTO();
        jobDTO.setId(jobId);
        jobDTO.setTitle("Software Engineer");
        jobDTO.setTechnology(Technology.JAVA);
        jobDTO.setMinExperience(3);
        jobDTO.setLocation(City.TUNIS);
    }

    @Test
    void shouldCreateJob() {
        when(jobMapper.toEntity(jobDTO)).thenReturn(job);
        when(jobService.createJob(job, partnerId)).thenReturn(job);
        when(jobMapper.toDTO(job)).thenReturn(jobDTO);

        ResponseEntity<JobDTO> response = jobController.createJob(partnerId, jobDTO);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("Software Engineer");

        verify(jobService, times(1)).createJob(job, partnerId);
    }

    @Test
    void shouldGetAllJobs() {
        when(jobService.getAllJobs()).thenReturn(List.of(job));
        when(jobMapper.toDTO(job)).thenReturn(jobDTO);

        ResponseEntity<List<JobDTO>> response = jobController.getAllJobs();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getTitle()).isEqualTo("Software Engineer");

        verify(jobService, times(1)).getAllJobs();
    }

    @Test
    void shouldGetJobById() {
        when(jobService.getJobById(jobId)).thenReturn(job);
        when(jobMapper.toDTO(job)).thenReturn(jobDTO);

        ResponseEntity<JobDTO> response = jobController.getJobById(jobId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(jobId);

        verify(jobService, times(1)).getJobById(jobId);
    }

    @Test
    void shouldDeleteJob() {
        doNothing().when(jobService).deleteJob(jobId);

        ResponseEntity<Void> response = jobController.deleteJob(jobId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(jobService, times(1)).deleteJob(jobId);
    }

    @Test
    void shouldGetMarketSync() {
        when(jobService.getAllJobs()).thenReturn(List.of(job));

        ResponseEntity<Map<String, Object>> response = jobController.getMarketSync(Technology.JAVA);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsKey("averageExperience");
        assertThat(response.getBody()).containsKey("scarcityIndex");
        assertThat(response.getBody()).containsKey("demandLevel");

        // With 1 job of JAVA, it represents 100% of jobs
        assertThat(response.getBody().get("averageExperience")).isEqualTo(3.0);
        assertThat(response.getBody().get("demandLevel")).isEqualTo("SATURATED");
    }
}
