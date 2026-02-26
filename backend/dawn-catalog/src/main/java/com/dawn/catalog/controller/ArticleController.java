package com.dawn.catalog.controller;

import com.dawn.catalog.dto.request.ArticleRequest;
import com.dawn.catalog.dto.response.ArticleResponse;
import com.dawn.catalog.service.ArticleService;
import com.dawn.common.core.dto.response.ResponseObject;
import com.dawn.common.core.dto.response.ResponsePage;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/article")
@RequiredArgsConstructor
public class ArticleController {
    private final ArticleService articleService;

    @GetMapping("")
    public ResponseObject<ResponsePage<ArticleResponse>> getAll(Pageable pageable) {
        return ResponseObject.success(articleService.getAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseObject<ArticleResponse> getOne(@PathVariable Long id) {
        return ResponseObject.success(articleService.getById(id));
    }

    @PostMapping("")
    public ResponseObject<ArticleResponse> create(@RequestBody ArticleRequest req) {
        return ResponseObject.created(articleService.create(req));
    }

    @PutMapping("/{id}")
    public ResponseObject<ArticleResponse> update(@PathVariable Long id, @RequestBody ArticleRequest req) {
        return ResponseObject.success(articleService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseObject<Void> delete(@PathVariable Long id) {
        articleService.delete(id);
        return ResponseObject.deleted();
    }

}
