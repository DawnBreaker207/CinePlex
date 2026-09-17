import {
  Component,
  computed,
  effect,
  inject,
  OnInit,
  untracked,
  ChangeDetectionStrategy,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { rxResource } from '@angular/core/rxjs-interop';
import { NzLayoutModule } from 'ng-zorro-antd/layout';
import { NzGridModule } from 'ng-zorro-antd/grid';
import { NzImageService } from 'ng-zorro-antd/image';
import { NzAlertModule } from 'ng-zorro-antd/alert';
import { NzSkeletonModule } from 'ng-zorro-antd/skeleton';
import { NzModalModule } from 'ng-zorro-antd/modal';
import { NzSelectModule } from 'ng-zorro-antd/select';
import { SliderComponent } from '@features/client/home/components/slider/slider.component';
import { HomeStore } from '@features/client/home/home.store';
import { RecommendationService } from '@domain/movie/data-access/recommendation.service';
import { Movie } from '@domain/movie/models/movie.model';
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
  styleUrl: './home.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HomeComponent implements OnInit {
  readonly homeStore = inject(HomeStore);
  private recommendations = inject(RecommendationService);

  readonly suggested = rxResource<Movie[], []>({
    stream: () => this.recommendations.recommend(6),
  });

  readonly suggestedOrFallback = computed(() => {
    const suggested = this.suggested.value() ?? [];
    if (suggested.length > 0) return suggested;
    return (this.homeStore.nowShowing() ?? []).slice(0, 6);
  });

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
