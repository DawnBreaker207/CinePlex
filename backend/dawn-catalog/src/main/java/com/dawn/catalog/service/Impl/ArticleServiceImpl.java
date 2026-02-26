package com.dawn.catalog.service.Impl;

import com.dawn.catalog.dto.request.ArticleRequest;
import com.dawn.catalog.dto.response.ArticleResponse;
import com.dawn.catalog.helper.ArticleMappingHelper;
import com.dawn.catalog.model.Article;
import com.dawn.catalog.repository.ArticleRepository;
import com.dawn.catalog.service.ArticleService;
import com.dawn.common.core.dto.response.ResponsePage;
import com.dawn.common.core.exception.wrapper.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class ArticleServiceImpl implements ArticleService {

    private final ArticleRepository articleRepository;

    @Override
    public ResponsePage<ArticleResponse> getAll(Pageable pageable) {
        return ResponsePage.of(articleRepository
                .findAll(pageable)
                .map(ArticleMappingHelper::map));
    }

    @Override
    public ArticleResponse getById(Long id) {
        return articleRepository
                .findById(id)
                .map(ArticleMappingHelper::map)
                .orElseThrow(() -> new ResourceNotFoundException("Article not found"));
    }

    @Override
    @Transactional
    public ArticleResponse create(ArticleRequest req) {
        Article article = ArticleMappingHelper.map(req);

        article.setAuthorId(1L);
        article.setSlug(generateSlug(article.getTitle()));

        return ArticleMappingHelper.map(articleRepository.save(article));
    }

    @Override
    @Transactional
    public ArticleResponse update(Long id, ArticleRequest req) {
        Article article = articleRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Article not found"));
        article.setSlug(generateSlug(article.getTitle()));
        article.setTitle(req.getTitle());
        article.setSummary(req.getSummary());
        article.setContent(req.getContent());
        article.setStatus(req.getStatus());
        return ArticleMappingHelper.map(articleRepository.save(article));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!articleRepository.existsById(id)) {
            throw new ResourceNotFoundException("Article not found");
        }

        articleRepository.deleteById(id);
    }

    private String generateSlug(String title) {
        return title.toLowerCase().replace(" ", "-");
    }
}
