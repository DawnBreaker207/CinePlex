package com.dawn.catalog.repository;

import com.dawn.catalog.model.Article;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {
    @Override
    Page<Article> findAll(Pageable pageable);

    Optional<Article> findBySlug(String slug);
}
