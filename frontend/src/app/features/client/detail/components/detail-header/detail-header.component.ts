import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { DatePipe } from '@angular/common';
import { NzTagModule } from 'ng-zorro-antd/tag';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { TranslatePipe } from '@ngx-translate/core';
import { Movie } from '@domain/movie/models/movie.model';

@Component({
  selector: 'app-detail-header',
  imports: [DatePipe, NzTagModule, NzButtonModule, NzIconModule, TranslatePipe],
  template: `
    <div class="relative h-[520px] w-full overflow-hidden">
      <div
        class="absolute inset-0 bg-cover bg-center"
        [style.backgroundImage]="
          'url(' +
          (movie().backdrop ||
            'https://placehold.co/1400x700/1A202C/F8FAFC?text=Dawn+Cinema') +
          ')'
        "
      ></div>
      <div
        class="absolute inset-0 bg-gradient-to-t from-[#111827] via-[#111827]/80 to-black/20"
      ></div>
      <div
        class="absolute inset-x-0 bottom-0 h-40 bg-gradient-to-t from-slate-50 to-transparent"
      ></div>

      <div
        class="relative mx-auto flex h-full max-w-5xl items-end gap-8 px-4 pb-10 md:px-6"
      >
        <div
          class="hidden w-56 shrink-0 translate-y-14 overflow-hidden rounded-xl border border-white/15 shadow-2xl md:block"
        >
          <img
            [src]="movie().poster || movie().backdrop"
            [alt]="movie().title"
            class="h-full w-full object-cover"
          />
        </div>

        <div class="mb-2 flex-1">
          <div
            class="mb-2 flex flex-wrap items-center gap-3 text-sm text-slate-200"
          >
            <span>{{ movie().releaseDate | date: 'dd/MM/yyyy' }}</span>
            <span>• {{ movie().duration }} phút</span>
          </div>

          <h1
            class="mb-2 font-['Bricolage_Grotesque'] text-4xl font-extrabold leading-tight text-white md:text-5xl"
          >
            {{ movie().title }}
          </h1>
          <p class="mb-4 text-lg italic text-slate-200/90 md:text-xl">
            {{ movie().originalTitle }}
          </p>

          <div class="mb-6 flex flex-wrap items-center gap-2">
            @for (genre of movie().genres; track genre) {
              <nz-tag
                [nzColor]="'transparent'"
                class="rounded-full border border-white/30 px-3 text-xs text-white/95"
              >
                {{ genre }}
              </nz-tag>
            }
          </div>

          <div class="flex flex-wrap gap-3">
            <button
              nz-button
              nzType="primary"
              class="rounded-lg border-none bg-[#E53E3E] px-6 font-semibold text-white hover:bg-[#C53030]"
            >
              {{ 'Đặt vé ngay' | translate }}
            </button>

            <button
              nz-button
              nzType="default"
              class="rounded-lg border border-white/50 bg-white/10 px-6 text-white hover:border-white hover:bg-white/20"
            >
              <nz-icon nzType="play-circle" nzTheme="fill" class="mr-2" />
              {{ 'Xem Trailer' | translate }}
            </button>
          </div>
        </div>
      </div>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DetailHeaderComponent {
  readonly movie = input.required<Movie>();
}
