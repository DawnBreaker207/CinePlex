import { Article, ArticleRequest } from '@domain/article/models/article.model';
import { Pagination } from '@core/models/common.model';
import {
  patchState,
  signalStore,
  withComputed,
  withMethods,
  withState,
} from '@ngrx/signals';
import { computed, inject } from '@angular/core';
import { ArticleService } from '@domain/article/data-access/article.service';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { pipe, switchMap, tap } from 'rxjs';
import { tapResponse } from '@ngrx/operators';
import { NzMessageService } from 'ng-zorro-antd/message';

export interface ArticleState {
  articles: Article[];
  selectArticle: Article | null;
  pagination: Pagination | null;
  loading: boolean;
  loadingDetails: boolean;
  saving: boolean;
  error: string | null;
}
export const initialState: ArticleState = {
  articles: [],
  selectArticle: null,
  pagination: null,
  loading: false,
  loadingDetails: false,
  saving: false,
  error: null,
};
export const ArticleStore = signalStore(
  { providedIn: 'root' },
  withState(initialState),
  withComputed((store) => ({
    articlesCount: computed(() => store.articles.length),
    hasArticles: computed(() => store.articles.length > 0),
    getArticleById: computed(
      () => (id: number) => store.articles().find((a) => a.id === id),
    ),
  })),

  withMethods(
    (
      store,
      articleService = inject(ArticleService),
      msg = inject(NzMessageService),
    ) => ({
      setSelectedArticle(article: Article | null) {
        patchState(store, { selectArticle: article, error: null });
      },
      //
      clearSelectedArticle() {
        patchState(store, { selectArticle: null, error: null });
      },
      //
      clearError() {
        patchState(store, { error: null });
      },
      //
      loadArticles: rxMethod<{ page: number; size: number }>(
        pipe(
          tap(() => patchState(store, { loading: true, error: null })),
          switchMap(({ page, size }) =>
            articleService.getAll({ page, size }).pipe(
              tapResponse({
                next: ({ content, pagination }) =>
                  patchState(store, {
                    articles: content,
                    pagination: pagination,
                    loading: false,
                  }),
                error: (error: any) =>
                  patchState(store, { error, loadingDetails: false }),
              }),
            ),
          ),
        ),
      ),
      //
      loadArticle: rxMethod<number>(
        pipe(
          tap(() => patchState(store, { loadingDetails: true, error: null })),
          switchMap((id) =>
            articleService.getOne(id).pipe(
              tapResponse({
                next: (article) => {
                  patchState(store, (state) => {
                    const existed = state.articles.findIndex(
                      (a: Article) => a.id === article.id,
                    );
                    const updatedArticles =
                      existed >= 0
                        ? state.articles.map((a) =>
                            a.id === article.id ? article : a,
                          )
                        : [...state.articles, article];
                    return {
                      articles: updatedArticles,
                      selectArticle: article,
                      loadingDetails: false,
                    };
                  });
                },
                error: (error: any) =>
                  patchState(store, { error, loadingDetails: false }),
              }),
            ),
          ),
        ),
      ),
      //
      createArticle: rxMethod<ArticleRequest>(
        pipe(
          tap(() => patchState(store, { saving: true, error: null })),
          switchMap((article) =>
            articleService.add(article).pipe(
              tapResponse({
                next: (article) => {
                  patchState(store, (state) => ({
                    articles: [article, ...state.articles],
                    saving: false,
                  }));
                  msg.success('Đăng bài viết mới thành công');
                },
                error: (error: any) => {
                  patchState(store, { error, loadingDetails: false });
                  msg.error('Đăng bài thất bại');
                },
              }),
            ),
          ),
        ),
      ),
      //
      updateArticle: rxMethod<{ id: number; article: ArticleRequest }>(
        pipe(
          tap(() => patchState(store, { saving: true, error: null })),
          switchMap(({ id, article }) =>
            articleService.update(id, article).pipe(
              tapResponse({
                next: (article) => {
                  patchState(store, (state) => ({
                    article:
                      state.articles.findIndex((a) => a.id === article.id) >= 0
                        ? state.articles.map((a) =>
                            a.id === article.id ? article : a,
                          )
                        : [...state.articles, article],
                    selectArticle: article,
                    saving: false,
                  }));
                  msg.success('Cập nhật bài viết thành công');
                },
                error: (error: any) => {
                  patchState(store, { error, loadingDetails: false });
                  msg.error('Cập nhật thất bại');
                },
              }),
            ),
          ),
        ),
      ),
      //
      deleteArticle: rxMethod<number>(
        pipe(
          tap(() => patchState(store, { saving: true, error: null })),
          switchMap((id) =>
            articleService.delete(id).pipe(
              tapResponse({
                next: () => {
                  patchState(store, (state) => ({
                    articles: state.articles.filter((a) => a.id !== id),
                    selectArticle:
                      state.selectArticle?.id === id
                        ? null
                        : state.selectArticle,
                    saving: false,
                  }));
                  msg.success('Đã xóa bài viết');
                },
                error: (error: any) => {
                  patchState(store, { error, loadingDetails: false });
                  msg.error('Xóa thất bại');
                },
              }),
            ),
          ),
        ),
      ),
      //
    }),
  ),
);
