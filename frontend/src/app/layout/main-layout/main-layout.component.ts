import {
  Component,
  inject,
  signal,
  ChangeDetectionStrategy,
} from '@angular/core';
import { NzContentComponent, NzLayoutComponent } from 'ng-zorro-antd/layout';
import {
  NavigationCancel,
  NavigationEnd,
  NavigationError,
  NavigationStart,
  RouteConfigLoadEnd,
  RouteConfigLoadStart,
  Router,
  RouterOutlet,
} from '@angular/router';
import { NzMenuModule } from 'ng-zorro-antd/menu';
import { HeaderComponent } from './header/header.component';
import { FooterComponent } from './footer/footer.component';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { LoadingComponent } from '@shared/components/loading/loading.component';
import { NzFloatButtonModule } from 'ng-zorro-antd/float-button';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'app-main-layout',
  imports: [
    FooterComponent,
    HeaderComponent,
    RouterOutlet,
    NzContentComponent,
    NzLayoutComponent,
    NzMenuModule,
    LoadingComponent,
    NzFloatButtonModule,
    TranslatePipe,
  ],
  templateUrl: './main-layout.component.html',
  styleUrl: './main-layout.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MainLayoutComponent {
  private router = inject(Router);
  loading = signal(false);

  constructor() {
    this.router.events.pipe(takeUntilDestroyed()).subscribe((event) => {
      if (
        event instanceof RouteConfigLoadStart ||
        event instanceof NavigationStart
      ) {
        this.loading.set(true);
      } else if (
        event instanceof RouteConfigLoadEnd ||
        event instanceof NavigationEnd ||
        event instanceof NavigationCancel ||
        event instanceof NavigationError
      ) {
        this.loading.set(false);
      }
    });
  }
}
