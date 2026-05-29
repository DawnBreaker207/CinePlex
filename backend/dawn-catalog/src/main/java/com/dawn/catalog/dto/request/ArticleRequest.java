package com.dawn.catalog.dto.request;


import com.dawn.catalog.constant.ArticleStatus;
import com.dawn.catalog.constant.ArticleType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ArticleRequest {
    private String title;

    private String thumbnail;

    private String summary;

    private String content;

    private ArticleType type;

    private ArticleStatus status;
}
