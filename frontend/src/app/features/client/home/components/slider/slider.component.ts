import {
  Component,
  input,
  inject,
  output,
  signal,
  ChangeDetectionStrategy,
} from '@angular/core';
import { NzCarouselModule } from 'ng-zorro-antd/carousel';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { Movie } from '@domain/movie/models/movie.model';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { ToastService } from '@core/services/toast/toast.service';
import dayjs from 'dayjs';

@Component({
  selector: 'app-slider',
  imports: [NzCarouselModule, NzButtonModule, NzIconModule, TranslatePipe],
  templateUrl: './slider.component.html',
  styleUrl: './slider.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SliderComponent {
  movies = input<Movie[]>([]);
  selectMovie = output<number>();
  autoPlay = signal(true);
  private translate = inject(TranslateService);
  private toast = inject(ToastService);

  private fallbackBanners = [
    {
      id: -1,
      movieId: null as number | null,
      title: 'Dune: Part Two',
      desc: 'The saga continues as Paul Atreides unites with Chani and the Fremen while on a warpath of revenge.',
      image: 'https://image.tmdb.org/t/p/w1280/xOMo8BRK7PfcJv9JCnx7s5hj0PX.jpg',
      isComingSoon: false,
    },
    {
      id: -2,
      movieId: null as number | null,
      title: 'Kung Fu Panda 4',
      desc: 'Po is gearing up to become the spiritual leader of his Valley of Peace.',
      image: 'https://image.tmdb.org/t/p/w1280/kYgQzzjNis5jJalYtIHgrom0gOx.jpg',
      isComingSoon: false,
    },
  ];

  get banners() {
    const list = this.movies();
    if (!list.length) return this.fallbackBanners;

    return list.slice(0, 5).map((movie) => ({
      id: movie.id,
      movieId: movie.id as number | null,
      title: movie.title,
      desc: movie.overview || this.translate.instant('movie.genreUpdating'),
      image:
        movie.backdrop ||
        movie.poster ||
        'https://placehold.co/1200x500/1A202C/F8FAFC?text=CinePlex',
      isComingSoon: dayjs(movie.releaseDate).isAfter(dayjs(), 'day'),
    }));
  }

  book(banner: { movieId: number | null }): void {
    if (banner.movieId != null) this.selectMovie.emit(banner.movieId);
  }

  soon(): void {
    this.toast.createNotification({
      type: 'info',
      title: this.translate.instant('common.soon'),
      message: this.translate.instant('common.soonMessage'),
    });
  }
}
