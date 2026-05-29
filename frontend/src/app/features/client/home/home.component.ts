import { Component, effect, inject, OnInit, untracked } from '@angular/core';
import { NzLayoutModule } from 'ng-zorro-antd/layout';
import { NzColDirective, NzGridModule, NzRowDirective } from 'ng-zorro-antd/grid';
import { NzImageService } from 'ng-zorro-antd/image';
import { NzModalModule, NzModalService } from 'ng-zorro-antd/modal';
import { NzEmptyComponent } from 'ng-zorro-antd/empty';
import { SliderComponent } from '@features/client/home/components/slider/slider.component';
import { HomeStore } from '@features/client/home/home.store';
import { TranslatePipe } from '@ngx-translate/core';
import { NzTabsModule } from 'ng-zorro-antd/tabs';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { MovieItemComponent } from '@shared/components/movie-item/movie-item.component';

@Component({
  selector: 'app-client',
  imports: [
    NzLayoutModule,
    NzRowDirective,
    NzColDirective,
    NzGridModule,
    NzEmptyComponent,
    NzModalModule,
    NzTabsModule,
    NzButtonModule,
    SliderComponent,
    TranslatePipe,
    MovieItemComponent,
  ],
  providers: [NzImageService, NzModalService, HomeStore],
  templateUrl: './home.component.html',
  styleUrl: './home.component.css',
})
export class HomeComponent implements OnInit {
  readonly homeStore = inject(HomeStore);

  constructor() {
    effect(() => {
      const theaterId = this.homeStore.selectedTheaterId();
      if (theaterId) {
        untracked(() => this.homeStore.loadShowtimes(theaterId));
      }
    });

    effect(() => {
      const list = this.homeStore.theaters();
      if (list.length > 0) {
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
