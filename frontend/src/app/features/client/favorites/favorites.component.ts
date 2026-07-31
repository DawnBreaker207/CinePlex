import {
  Component,
  ChangeDetectionStrategy,
  inject,
} from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { map, of } from 'rxjs';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzGridModule } from 'ng-zorro-antd/grid';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzToolTipModule } from 'ng-zorro-antd/tooltip';
import { NzEmptyModule } from 'ng-zorro-antd/empty';
import { TranslatePipe } from '@ngx-translate/core';
import { MovieService } from '@domain/movie/data-access/movie.service';
import { Movie } from '@domain/movie/models/movie.model';
import { FavoritesService } from '@features/client/favorites/favorites.service';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-favorites',
  imports: [NzButtonModule, NzGridModule, NzIconModule, NzEmptyModule, NzToolTipModule, TranslatePipe, RouterLink],
  templateUrl: './favorites.component.html',
  styleUrls: ['./favorites.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FavoritesComponent {
  private movieService = inject(MovieService);
  private favoritesService = inject(FavoritesService);

  readonly favorites = rxResource<Movie[], number[]>({
    params: () => this.favoritesService.ids(),
    stream: ({ params: ids }) =>
      ids.length
        ? this.movieService.getMovieLists({ size: 100 }).pipe(
            map((page) => {
              const byId = new Map(page.content.map((m) => [m.id, m]));
              return ids.map((id) => byId.get(id)).filter((m): m is Movie => !!m);
            }),
          )
        : of([]),
  });

  remove(id: number) {
    this.favoritesService.toggle(id);
  }
}
