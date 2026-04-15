package com.example.msforum.dto;

import com.example.msforum.entities.PostCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PostRequest {

    @NotNull
    private String userId;

    private Long formationId;

    @NotBlank
    private String title;

    @NotBlank
    private String content;

    private PostCategory category;

    private String imageUrl;
}
