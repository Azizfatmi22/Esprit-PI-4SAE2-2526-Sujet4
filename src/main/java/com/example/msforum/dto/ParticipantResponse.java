package com.example.msforum.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ParticipantResponse {
    private Long id;
    private String name;
    private String email;
}

