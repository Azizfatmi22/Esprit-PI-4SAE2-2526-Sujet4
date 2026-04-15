package com.example.msforum.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ParticipantRequest {
    @NotBlank
    private String name;

    @NotBlank
    @Email
    private String email;
}

