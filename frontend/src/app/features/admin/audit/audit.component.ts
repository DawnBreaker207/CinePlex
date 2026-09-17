import {
  Component,
  inject,
  OnInit,
  signal,
  ChangeDetectionStrategy,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { NzTableModule, NzTableQueryParams } from 'ng-zorro-antd/table';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzSelectModule } from 'ng-zorro-antd/select';
import { NzTagModule } from 'ng-zorro-antd/tag';
import { LoadingComponent } from '@shared/components/loading/loading.component';
import { ErrorStateComponent } from '@shared/components/error-state/error-state.component';
import { TranslatePipe } from '@ngx-translate/core';
import { AuditService } from '@domain/audit/data-access/audit.service';
import { AuditLog } from '@domain/audit/models/audit.model';
import { Pagination } from '@core/models/common.model';
import { errorMessage } from '@shared/utils/error.helper';

@Component({
  selector: 'app-audit',
  imports: [
    FormsModule,
    DatePipe,
    NzTableModule,
    NzInputModule,
    NzButtonModule,
    NzSelectModule,
    NzTagModule,
    LoadingComponent,
    ErrorStateComponent,
    TranslatePipe,
  ],
  templateUrl: './audit.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AuditComponent implements OnInit {
  private auditService = inject(AuditService);

  readonly logs = signal<AuditLog[]>([]);
  readonly pagination = signal<Pagination | null>(null);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  readonly action = signal('');
  readonly entity = signal('');
  readonly status = signal('');

  pageIndex = 1;
  pageSize = 10;

  ngOnInit() {
    this.load();
  }

  load() {
    this.loading.set(true);
    this.error.set(null);
    this.auditService
      .search({
        action: this.action().trim() || undefined,
        entity: this.entity().trim() || undefined,
        status: this.status() || undefined,
        page: this.pageIndex - 1,
        size: this.pageSize,
      })
      .subscribe({
        next: (page) => {
          this.logs.set(page.content ?? []);
          this.pagination.set(page.pagination);
          this.loading.set(false);
        },
        error: (err) => {
          this.error.set(errorMessage(err));
          this.loading.set(false);
        },
      });
  }

  clear() {
    this.action.set('');
    this.entity.set('');
    this.status.set('');
    this.pageIndex = 1;
    this.load();
  }

  onQueryParamsChange(params: NzTableQueryParams) {
    this.pageIndex = params.pageIndex;
    this.pageSize = params.pageSize;
    this.load();
  }

  isRisky(action: string): boolean {
    return /REFUND|DELETE|UPDATE|REMOVE|GRANT/i.test(action);
  }
}
