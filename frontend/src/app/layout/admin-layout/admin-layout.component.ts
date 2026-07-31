import {
  Component,
  DestroyRef,
  inject,
  OnInit,
  signal,
  ChangeDetectionStrategy,
} from '@angular/core';
import {
  ActivatedRoute,
  NavigationEnd,
  Router,
  RouterOutlet,
} from '@angular/router';
import { filter } from 'rxjs';
import {
  NzContentComponent,
  NzFooterComponent,
  NzLayoutModule,
} from 'ng-zorro-antd/layout';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzMenuModule } from 'ng-zorro-antd/menu';
import { NzAvatarModule } from 'ng-zorro-antd/avatar';
import { NzDropDownModule } from 'ng-zorro-antd/dropdown';
import { NzBadgeModule } from 'ng-zorro-antd/badge';
import { NzBreadCrumbModule } from 'ng-zorro-antd/breadcrumb';
import { SidebarComponent } from './sidebar/sidebar.component';
import { NavbarComponent } from './navbar/navbar.component';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { TranslatePipe } from '@ngx-translate/core';
import { ChatComponent } from '@shared/components/chat/chat.component';

export interface Breadcrumb {
  label: string;
  url: string;
}

@Component({
  selector: 'app-admin-layout',
  imports: [
    NzContentComponent,
    NzIconModule,
    NzLayoutModule,
    NzMenuModule,
    RouterOutlet,
    NzFooterComponent,
    NzAvatarModule,
    NzDropDownModule,
    NzBadgeModule,
    NzBreadCrumbModule,
    SidebarComponent,
    NavbarComponent,
    TranslatePipe,
    ChatComponent,
  ],
  templateUrl: './admin-layout.component.html',
  styleUrl: './admin-layout.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AdminLayoutComponent implements OnInit {
  private router = inject(Router);
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);

  ngOnInit() {
    // Tạo breadcrumb lần đầu
    this.breadcrumbs.set(this.createBreadcrumbs(this.activatedRoute.root));

    // Lắng nghe sự kiện đổi route để cập nhật lại breadcrumb
    this.router.events
      .pipe(
        filter((event) => event instanceof NavigationEnd),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => {
        this.breadcrumbs.set(this.createBreadcrumbs(this.activatedRoute.root));
      });
  }

  breadcrumbs = signal<Breadcrumb[]>([]);
  isCollapsed = false;
  protected readonly date = new Date();

  toggleCollapse(collapsed: boolean) {
    this.isCollapsed = collapsed;
  }

  // Hàm đệ quy để duyệt cây Router và lấy data.breadcrumb
  private createBreadcrumbs(
    route: ActivatedRoute,
    url: string = '',
    breadcrumbs: Breadcrumb[] = [],
  ): Breadcrumb[] {
    const children: ActivatedRoute[] = route.children;

    if (children.length === 0) {
      return breadcrumbs;
    }

    for (const child of children) {
      const routeURL: string = child.snapshot.url
        .map((segment) => segment.path)
        .join('/');

      if (routeURL !== '') {
        url += `/${routeURL}`;
      }

      // Chỉ lấy những route có data.breadcrumb
      const label = child.snapshot.data['breadcrumb'];
      if (label) {
        breadcrumbs.push({ label, url });
      }

      return this.createBreadcrumbs(child, url, breadcrumbs);
    }
    return breadcrumbs;
  }
}
