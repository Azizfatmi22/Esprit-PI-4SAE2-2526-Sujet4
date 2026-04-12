package com.formini.msliveclass.dto;

import java.util.List;

public class ChapterDTO {
    private Long id;
    private String title;
    private String description;
    private Integer orderIndex;
    private List<ContentBlockDTO> contentBlocks;

    public ChapterDTO() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(Integer orderIndex) {
        this.orderIndex = orderIndex;
    }

    public List<ContentBlockDTO> getContentBlocks() {
        return contentBlocks;
    }

    public void setContentBlocks(List<ContentBlockDTO> contentBlocks) {
        this.contentBlocks = contentBlocks;
    }
}
