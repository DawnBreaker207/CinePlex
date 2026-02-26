package com.dawn.catalog.dto.response;

import com.dawn.catalog.config.ArticleStatus;
import com.dawn.catalog.config.ArticleType;
import com.dawn.common.core.dto.response.BaseResponse;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ArticleResponse extends BaseResponse {
    private Long id;

    private String title;

    private String slug;

    private String summary;

    private String thumbnail;

    private String content;

    private String author;

    private ArticleStatus status;

    private ArticleType type;

    private Long views;

    private Boolean isDeleted;
}
