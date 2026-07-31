import {
  Component,
  input,
  output,
  ChangeDetectionStrategy,
} from '@angular/core';
import { NgOptimizedImage } from '@angular/common';
import { NzButtonComponent } from 'ng-zorro-antd/button';
import { RouterLink } from '@angular/router';
import { NzTypographyModule } from 'ng-zorro-antd/typography';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { Movie } from '@domain/movie/models/movie.model';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'app-movie-item',
  imports: [
    RouterLink,
    NzIconModule,
    NzButtonComponent,
    NzTypographyModule,
    NgOptimizedImage,
    TranslatePipe,
  ],
  templateUrl: './movie-item.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MovieItemComponent {
  movie = input.required<Movie>();
  canBook = input(true);
  selectMovie = output<number>();

  onSelectMovie(id: number) {
    if (!this.canBook()) return;
    this.selectMovie.emit(id);
  }
}
