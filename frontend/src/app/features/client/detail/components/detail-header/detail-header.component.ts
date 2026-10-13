import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  input,
  output,
  signal,
} from '@angular/core';
import { DatePipe } from '@angular/common';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { NzTagModule } from 'ng-zorro-antd/tag';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzModalModule } from 'ng-zorro-antd/modal';
import { TranslatePipe } from '@ngx-translate/core';
import { Movie } from '@domain/movie/models/movie.model';

@Component({
  selector: 'app-detail-header',
  imports: [
    DatePipe,
    NzTagModule,
    NzButtonModule,
    NzIconModule,
    NzModalModule,
    TranslatePipe,
  ],
  templateUrl: './detail-header.component.html',
  styleUrls: ['./detail-header.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DetailHeaderComponent {
  readonly movie = input.required<Movie>();
  readonly bookNow = output<void>();
  private readonly sanitizer = inject(DomSanitizer);

  readonly isTrailerOpen = signal(false);

  readonly trailerUrl = computed<SafeResourceUrl>(() => {
    const movie = this.movie();
    // Search query on youtube-nocookie embed
    const q = encodeURIComponent(`${movie.title} official trailer`);
    return this.sanitizer.bypassSecurityTrustResourceUrl(
      `https://www.youtube-nocookie.com/embed?listType=search&list=${q}&autoplay=1`,
    );
  });

  openTrailer(): void {
    this.isTrailerOpen.set(true);
  }

  closeTrailer(): void {
    this.isTrailerOpen.set(false);
  }
}
