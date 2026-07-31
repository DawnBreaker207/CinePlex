import {
  ChangeDetectionStrategy,
  Component,
  input,
  output,
} from '@angular/core';
import { DatePipe, NgClass } from '@angular/common';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { TranslatePipe } from '@ngx-translate/core';

export interface CinemaShowtime {
  id: number;
  time: string;
  availableSeats: number;
  totalSeats: number;
}

export interface CinemaGroup {
  id: number;
  name: string;
  showtimes: CinemaShowtime[];
}

@Component({
  selector: 'app-showtime-section',
  imports: [DatePipe, NgClass, NzButtonModule, NzIconModule, TranslatePipe],
  template: `
    <div class="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
      <h2
        class="mb-6 border-l-4 border-[#E53E3E] pl-3 font-['Bricolage_Grotesque'] text-2xl font-bold text-slate-900"
      >
        {{ 'detail.showtimes' | translate }}
      </h2>

      <div class="mb-6 flex gap-3 overflow-x-auto pb-2 scrollbar-hide">
        @for (date of dates(); track date) {
          <div
            (click)="selectDate(date)"
            (keyup)="selectDate(date)"
            tabindex="0"
            role="button"
            class="min-w-[84px] cursor-pointer rounded-xl border p-3 text-center transition-all duration-200"
            [ngClass]="
              (selectedDate() | date: 'ddMMyy') === (date | date: 'ddMMyy')
                ? 'border-[#E53E3E] bg-[#E53E3E] text-white'
                : 'border-slate-200 bg-slate-50 text-slate-500 hover:border-slate-300'
            "
          >
            <span class="text-2xl font-bold">{{ date | date: 'dd' }}</span>
            /<span class="text-sm">{{ date | date: 'MM' }}</span>
          </div>
        }
      </div>

      <div class="space-y-4">
        @for (cinema of cinemas(); track cinema.id) {
          <div
            class="rounded-xl border border-slate-200 bg-slate-50 p-4 transition hover:border-slate-300"
          >
            <p class="mb-3 text-sm font-semibold text-slate-800">
              {{ cinema.name }}
            </p>
            <div class="flex flex-wrap gap-3">
              @for (show of cinema.showtimes; track show.time) {
                <div
                  class="group relative w-[112px] cursor-pointer overflow-hidden rounded-lg border border-slate-300 bg-white p-3 text-center transition-all hover:border-[#E53E3E]"
                >
                  <div
                    class="text-lg font-bold text-slate-900 group-hover:text-[#E53E3E]"
                  >
                    {{ show.time }}
                  </div>
                  <div class="flex items-center gap-1.5 mt-1">
                    <div class="h-1.5 w-1.5 rounded-full bg-emerald-500"></div>
                    <span
                      class="text-[10px] font-medium text-slate-500 group-hover:text-[#E53E3E]"
                    >
                      {{
                        show.availableSeats > 0
                          ? show.availableSeats +
                            ('detail.seatsAvailable' | translate)
                          : ('detail.soldOut' | translate)
                      }}
                    </span>
                  </div>
                </div>
              }
            </div>
          </div>
        } @empty {
          <div
            class="flex flex-col items-center justify-center rounded-2xl border-2 border-dashed border-slate-200 bg-slate-50 py-16"
          >
            <nz-icon
              nzType="calendar"
              class="mb-4 text-5xl text-slate-400"
            ></nz-icon>
            <p class="text-lg text-slate-500">
              {{ 'detail.noShowtimeToday' | translate }}
            </p>
            <button
              nz-button
              nzType="link"
              (click)="selectDate(dates()[0])"
              class="text-[#E53E3E]"
            >
              {{ 'detail.otherDates' | translate }}
            </button>
          </div>
        }
      </div>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ShowtimeSectionComponent {
  readonly dates = input.required<Date[]>();
  readonly selectedDate = input.required<Date>();
  readonly cinemas = input.required<CinemaGroup[]>();
  readonly dateChange = output<Date>();

  selectDate(date: Date) {
    this.dateChange.emit(date);
  }
}
