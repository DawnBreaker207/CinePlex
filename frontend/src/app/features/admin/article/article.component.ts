import { Component, inject } from '@angular/core';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzTableModule, NzTableQueryParams } from 'ng-zorro-antd/table';

import { NzTooltipModule } from 'ng-zorro-antd/tooltip';
import { NzTagModule } from 'ng-zorro-antd/tag';
import { DatePipe } from '@angular/common';
import { NzDrawerModule, NzDrawerService } from 'ng-zorro-antd/drawer';
import { NzPopconfirmModule } from 'ng-zorro-antd/popconfirm';
import { FormsModule } from '@angular/forms';
import { Article } from '@domain/article/models/article.model';
import { ArticleFormComponent } from '@features/admin/article/form/article-form.component';
import { TranslateModule } from '@ngx-translate/core';
import { HEADER_COLUMNS } from '@core/constants/column';
import { ArticleStore } from '@domain/article/data-access/article.store';
import { LoadingComponent } from '@shared/components/loading/loading.component';

@Component({
  selector: 'app-article',
  imports: [
    NzButtonModule,
    NzIconModule,
    NzTableModule,
    NzTooltipModule,
    NzTagModule,
    DatePipe,
    NzDrawerModule,
    FormsModule,
    NzPopconfirmModule,
    TranslateModule,
    LoadingComponent,
  ],
  templateUrl: './article.component.html',
  styleUrl: './article.component.css',
})
export class ArticleComponent {
  readonly articleStore = inject(ArticleStore);
  private drawerService = inject(NzDrawerService);

  headerColumn = HEADER_COLUMNS.ARTICLE;
  pageIndex = 1;
  pageSize = 10;

  openDrawer(article?: Article) {
    const isEditMode = !!article;
    this.drawerService.create<
      ArticleFormComponent,
      { article: Article | null; isEditMode: boolean }
    >({
      nzTitle: isEditMode ? 'Chỉnh sửa bài viét' : 'Viết bài mới',
      nzContent: ArticleFormComponent,
      nzWidth: 800,
      nzData: {
        article: article || null,
        isEditMode: isEditMode,
      },
    });
  }

  deleteArticle(id: number) {
    this.articleStore.deleteArticle(id);
  }
  loadArticles() {
    this.articleStore.loadArticles({
      page: this.pageIndex - 1,
      size: this.pageSize,
    });
  }
  onQueryParamsChange(params: NzTableQueryParams) {
    const { pageIndex, pageSize } = params;
    this.pageIndex = pageIndex;
    this.pageSize = pageSize;
    this.loadArticles();
  }
}
