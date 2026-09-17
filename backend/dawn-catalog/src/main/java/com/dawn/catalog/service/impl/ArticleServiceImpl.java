package com.dawn.catalog.service.impl;

import com.dawn.catalog.dto.request.ArticleRequest;
import com.dawn.catalog.dto.response.ArticleResponse;
import com.dawn.catalog.helper.ArticleMappingHelper;
import com.dawn.catalog.model.Article;
import com.dawn.catalog.repository.ArticleRepository;
import com.dawn.catalog.service.ArticleService;
import com.dawn.common.core.annotation.AuditLog;
import com.dawn.common.core.constant.LogConstant;
import com.dawn.common.core.dto.response.ResponsePage;
import com.dawn.common.core.exception.wrapper.ResourceNotFoundException;
import com.dawn.common.core.service.AuditLogService;
import com.dawn.common.core.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArticleServiceImpl implements ArticleService {

    private final ArticleRepository articleRepository;
    private final AuditLogService auditLogService;

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
    @AuditLog(action = LogConstant.Action.ARTICLE_CREATE, entity = LogConstant.Entity.ARTICLE)
    public ArticleResponse create(ArticleRequest req) {
        String slug = generateSlug(req.getTitle());
        Optional<Article> existing = articleRepository.findBySlug(slug);
        if (existing.isPresent() && existing.get().getIsActive()) {
            // slug already used by an active article; let the UNIQUE constraint reject the duplicate
            return ArticleMappingHelper.map(articleRepository.save(ArticleMappingHelper.map(req)));
        }
        if (existing.isPresent()) {
            // Reactivate soft-deleted article with the same slug; created_at is preserved
            Article article = existing.get();
            article.setTitle(req.getTitle());
            article.setSlug(slug);
            article.setSummary(req.getSummary());
            article.setThumbnail(req.getThumbnail());
            article.setContent(req.getContent());
            article.setType(req.getType());
            article.setStatus(req.getStatus());
            article.setIsActive(true);
            auditLogService.record("ARTICLE_REACTIVATED", "ARTICLE", article.getId().toString(), null,
                    "INACTIVE", "ACTIVE", "slug=" + slug,
                    LogConstant.Status.SUCCESS, AuditLogService.clientIp(), null, null);
            log.info("Article reactivated: slug={}", slug);
            return ArticleMappingHelper.map(articleRepository.save(article));
        }
        Article article = ArticleMappingHelper.map(req);

        Long authorId = SecurityUtils.getCurrentUserId();
        if (authorId != null) {
            article.setAuthorId(authorId);
        }
        article.setSlug(slug);

        return ArticleMappingHelper.map(articleRepository.save(article));
    }

    @Override
    @Transactional
    @AuditLog(action = LogConstant.Action.ARTICLE_UPDATE, entity = LogConstant.Entity.ARTICLE,
            entityId = "#id", entityClass = Article.class, metadata = "'title=' + #req.title")
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
    @AuditLog(action = LogConstant.Action.ARTICLE_DELETE, entity = LogConstant.Entity.ARTICLE,
            entityId = "#id", entityClass = Article.class)
    public void delete(Long id) {
        if (!articleRepository.existsById(id)) {
            throw new ResourceNotFoundException("Article not found");
        }

        articleRepository.deleteById(id);
    }

    private String generateSlug(String title) {
        return title.toLowerCase()
                .replace(" ", "-")
                .replaceAll("[^a-zA-Z0-9-]", "");
    }
}
