import {
  Component,
  inject,
  signal,
  ChangeDetectionStrategy,
} from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { NzModalModule, NzModalRef } from 'ng-zorro-antd/modal';
import { NzListModule } from 'ng-zorro-antd/list';
import { NzAvatarModule } from 'ng-zorro-antd/avatar';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzSpinComponent } from 'ng-zorro-antd/spin';
import { MovieService, TMDBMovie } from '@domain/movie/data-access/movie.service';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'app-movie-form-api',
  imports: [
    ReactiveFormsModule,
    DatePipe,
    NzModalModule,
    NzListModule,
    NzAvatarModule,
    NzInputModule,
    NzButtonModule,
    NzIconModule,
    NzSpinComponent,
    TranslatePipe,
  ],
  templateUrl: './api-form.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FormMovieAPIComponent {
  private fb = inject(NonNullableFormBuilder);
  private movieService = inject(MovieService);
  private modalRef = inject(NzModalRef);

  searchCtrl = this.fb.control('');
  searchResults = signal<TMDBMovie[]>([]);
  isLoading = signal<boolean>(false);

  onSearch() {
    const query = this.searchCtrl.value?.trim();
    if (!query) {
      this.searchResults.set([]);
      return;
    }
    this.isLoading.set(true);
    this.movieService.searchMovies(query).subscribe({
      next: (res) => {
        this.isLoading.set(false);
        this.searchResults.set(res);
      },
      error: () => {
        this.isLoading.set(false);
      },
    });
  }

  selectMovie(movie: TMDBMovie) {
    this.modalRef.destroy(movie);
  }
}
