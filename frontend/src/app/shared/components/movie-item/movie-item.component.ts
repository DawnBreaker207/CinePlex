import {
  Component,
  inject,
  input,
  output,
  ChangeDetectionStrategy,
} from '@angular/core';
import { NzButtonComponent } from 'ng-zorro-antd/button';
import { RouterLink } from '@angular/router';
import { NzTypographyModule } from 'ng-zorro-antd/typography';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzToolTipModule } from 'ng-zorro-antd/tooltip';
import { Movie } from '@domain/movie/models/movie.model';
import { TranslatePipe } from '@ngx-translate/core';
import { FavoritesService } from '@features/client/favorites/favorites.service';

@Component({
  selector: 'app-movie-item',
  imports: [
    RouterLink,
    NzIconModule,
    NzButtonComponent,
NzTypographyModule,
NzToolTipModule,
    TranslatePipe,
  ],
  templateUrl: './movie-item.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MovieItemComponent {
  favorites = inject(FavoritesService);
  movie = input.required<Movie>();
  canBook = input(true);
  selectMovie = output<number>();

  onSelectMovie(id: number) {
    if (!this.canBook()) return;
    this.selectMovie.emit(id);
  }

  toggleFavorite() {
    this.favorites.toggle(this.movie().id);
  }
}
