import {
  Component,
  effect,
  inject,
  OnInit,
  untracked,
  ChangeDetectionStrategy,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { NzLayoutModule } from 'ng-zorro-antd/layout';
import { NzGridModule } from 'ng-zorro-antd/grid';
import { NzImageService } from 'ng-zorro-antd/image';
import { NzAlertModule } from 'ng-zorro-antd/alert';
import { NzSkeletonModule } from 'ng-zorro-antd/skeleton';
import { NzModalModule } from 'ng-zorro-antd/modal';
import { NzSelectModule } from 'ng-zorro-antd/select';
import { SliderComponent } from '@features/client/home/components/slider/slider.component';
import { HomeStore } from '@features/client/home/home.store';
import { TranslatePipe } from '@ngx-translate/core';
import { NzTabsModule } from 'ng-zorro-antd/tabs';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { MovieItemComponent } from '@shared/components/movie-item/movie-item.component';

@Component({
  selector: 'app-client',
  imports: [
    RouterLink,
    NzLayoutModule,
    NzGridModule,
    NzTabsModule,
    NzButtonModule,
    NzAlertModule,
    NzSkeletonModule,
    NzModalModule,
    NzSelectModule,
    SliderComponent,
    TranslatePipe,
    MovieItemComponent,
  ],
  providers: [NzImageService, HomeStore],
  templateUrl: './home.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HomeComponent implements OnInit {
  readonly homeStore = inject(HomeStore);

  constructor() {
    effect(() => {
      if (this.homeStore.theaters().length > 0) {
        untracked(() => this.homeStore.initData());
      }
    });
  }

  ngOnInit() {
    this.homeStore.initData();
  }

  onSelectedMovie(movieId: number) {
    this.homeStore.handleMovieSelection(movieId);
  }
}
