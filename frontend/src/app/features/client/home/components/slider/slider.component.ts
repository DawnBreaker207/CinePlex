import { Component, input } from '@angular/core';
import { NzCarouselModule } from 'ng-zorro-antd/carousel';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { Movie } from '@domain/movie/models/movie.model';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'app-slider',
  imports: [NzCarouselModule, NzButtonModule, NzIconModule, TranslatePipe],
  templateUrl: './slider.component.html',
  styleUrl: './slider.component.css',
})
export class SliderComponent {
  movies = input<Movie[]>([]);

  private fallbackBanners = [
    {
      id: 1,
      title: 'Dune: Part Two',
      desc: 'The saga continues as Paul Atreides unites with Chani and the Fremen while on a warpath of revenge.',
      image:
        'https://image.tmdb.org/t/p/original/xOMo8BRK7PfcJv9JCnx7s5hj0PX.jpg',
    },
    {
      id: 2,
      title: 'Kung Fu Panda 4',
      desc: 'Po is gearing up to become the spiritual leader of his Valley of Peace.',
      image:
        'https://image.tmdb.org/t/p/original/kYgQzzjNis5jJalYtIHgrom0gOx.jpg',
    },
  ];

  get banners() {
    const list = this.movies();
    if (!list.length) return this.fallbackBanners;

    return list.slice(0, 5).map((movie) => ({
      id: movie.id,
      title: movie.title,
      desc: movie.overview || 'Nội dung phim đang được cập nhật.',
      image:
        movie.backdrop ||
        movie.poster ||
        'https://placehold.co/1200x500/1A202C/F8FAFC?text=Dawn+Cinema',
    }));
  }
}
