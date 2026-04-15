package com.example.msforum.dto;

import com.example.msforum.entities.ReactionType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReactionRequest {

    @NotNull
    private String userId;

    @NotNull
    private ReactionType type;
}
