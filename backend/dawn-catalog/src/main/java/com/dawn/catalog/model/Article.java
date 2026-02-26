package com.dawn.catalog.model;

import com.dawn.catalog.config.ArticleStatus;
import com.dawn.catalog.config.ArticleType;
import com.dawn.common.core.model.AbstractMappedEntity;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Hidden
@Table(name = "article")
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = false)
public class Article extends AbstractMappedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "title")
    private String title;

    @Column(name = "slug", unique = true)
    private String slug;

    @Column(name = "summary")
    private String summary;

    @Column(name = "thumbnail")
    private String thumbnail;

    @Column(name = "content")
    private String content;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private ArticleStatus status;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    private ArticleType type;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    @Column(name = "views")
    private Long views;
}
