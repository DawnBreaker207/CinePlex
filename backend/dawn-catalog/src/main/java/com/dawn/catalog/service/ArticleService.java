package com.dawn.catalog.service;

import com.dawn.catalog.dto.request.ArticleRequest;
import com.dawn.catalog.dto.response.ArticleResponse;
import com.dawn.common.core.dto.response.ResponsePage;
import org.springframework.data.domain.Pageable;

public interface ArticleService {
    ResponsePage<ArticleResponse> getAll(Pageable pageable);

    ArticleResponse getById(Long id);

    ArticleResponse create(ArticleRequest req);

    ArticleResponse update(Long id, ArticleRequest req);

    void delete(Long id);
}
