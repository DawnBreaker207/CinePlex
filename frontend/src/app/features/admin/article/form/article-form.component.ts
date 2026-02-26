import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { QuillEditorComponent } from 'ngx-quill';
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzSelectModule } from 'ng-zorro-antd/select';
import { NzUploadModule } from 'ng-zorro-antd/upload';
import { NzIconModule } from 'ng-zorro-antd/icon';
import {
  Article,
  ArticleStatus,
  ArticleType,
} from '@domain/article/models/article.model';
import { TranslatePipe } from '@ngx-translate/core';
import { quillConfig } from '@core/config/quill.config';
import { NZ_DRAWER_DATA, NzDrawerRef } from 'ng-zorro-antd/drawer';
import { ArticleStore } from '@domain/article/data-access/article.store';

@Component({
  selector: 'app-form',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    QuillEditorComponent,
    NzFormModule,
    NzInputModule,
    NzButtonModule,
    NzSelectModule,
    NzUploadModule,
    NzIconModule,
    TranslatePipe,
  ],
  templateUrl: './article-form.component.html',
  styleUrl: './article-form.component.css',
})
export class ArticleFormComponent implements OnInit {
  protected readonly quillConfig = quillConfig;
  private drawerRef = inject(NzDrawerRef);
  private articleStore = inject(ArticleStore);
  readonly nzData = inject<{ article: Article | null; isEditMode: boolean }>(
    NZ_DRAWER_DATA,
  );
  protected readonly ArticleType = ArticleType;
  protected readonly ArticleStatus = ArticleStatus;
  isLoading = this.articleStore.saving;
  form!: FormGroup;
  private fb = inject(FormBuilder);

  ngOnInit() {
    this.initForm(this.nzData.article);
  }

  initForm(data: Article | null) {
    this.form = this.fb.group({
      title: [data?.title || '', [Validators.required]],
      thumbnail: [data?.thumbnail || '', [Validators.required]],
      summary: [data?.summary || '', [Validators.required]],
      content: [data?.content || '', [Validators.required]],
      type: [data?.type || 'UNKNOWN'],
      status: [data?.status || 'DRAFT'],
    });
  }
  close() {
    this.drawerRef.close();
  }

  submit() {
    if (this.form.invalid) {
      Object.values(this.form.controls).forEach((control) => {
        if (control.invalid) {
          control.markAsDirty();
          control.updateValueAndValidity({ onlySelf: true });
        }
      });
      return;
    }

    const formData = this.form.value;
    if (this.nzData.isEditMode && this.nzData.article) {
      this.articleStore.updateArticle({
        id: this.nzData.article.id,
        article: formData,
      });
    } else {
      this.articleStore.createArticle(formData);
    }
    this.close();
  }
}
