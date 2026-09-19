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
    NzLayoutModule,
    RouterOutlet,
    NzFooterComponent,
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
    this.breadcrumbs.set(this.createBreadcrumbs(this.activatedRoute.root));

    // Rebuild breadcrumbs on route change
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

  // Walk the Router tree collecting data.breadcrumb
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

      // Only routes carrying data.breadcrumb
      const label = child.snapshot.data['breadcrumb'];
      if (label) {
        breadcrumbs.push({ label, url });
      }

      return this.createBreadcrumbs(child, url, breadcrumbs);
    }
    return breadcrumbs;
  }
}
