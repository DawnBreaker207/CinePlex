import {
  Component,
  computed,
  inject,
  OnInit,
  signal,
  ChangeDetectionStrategy,
} from '@angular/core';
import { NzModalRef } from 'ng-zorro-antd/modal';
import { DatePipe, NgClass } from '@angular/common';
import { NzButtonComponent } from 'ng-zorro-antd/button';
import { Showtime } from '@domain/showtime/models/showtime.model';
import { formatDate } from '@shared/utils/date.helper';
import { NzIconDirective } from 'ng-zorro-antd/icon';
import { TranslatePipe } from '@ngx-translate/core';
import dayjs from 'dayjs';

@Component({
  selector: 'app-theater',
  imports: [
    NgClass,
    NzButtonComponent,
    NzIconDirective,
    DatePipe,
    TranslatePipe,
  ],
  templateUrl: './modal.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ShowtimeModalComponent implements OnInit {
  private modalRef = inject(NzModalRef);

  showtimes = signal<Showtime[]>([]);
  movieTitle = signal<string | null>(null);
  selectedShowtimeId = signal<number | null>(null);

  groupShowtimes = computed(() => {
    const listShowtimes = this.showtimes();
    const groupsDate: { label: string; rawDate: Date; items: Showtime[] }[] =
      [];

    listShowtimes.forEach((st) => {
      const raw = new Date(st.showDate);
      const label = formatDate(st.showDate);
      const rawDate = new Date(
        raw.getFullYear(),
        raw.getMonth(),
        raw.getDate(),
      );
      let groupDay = groupsDate.find(
        (g) => g.rawDate.getTime() === rawDate.getTime(),
      );
      if (!groupDay) {
        groupDay = { label, rawDate, items: [] };
        groupsDate.push(groupDay);
      }
      groupDay.items.push(st);
    });

    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const tomorrow = new Date(today);
    tomorrow.setDate(tomorrow.getDate() + 1);

    return groupsDate.sort((a, b) => a.rawDate.getTime() - b.rawDate.getTime());
  });

  ngOnInit() {
    const { showtimes, movieTitle } = this.modalRef.getConfig().nzData;
    this.showtimes.set(showtimes);
    if (movieTitle) this.movieTitle.set(movieTitle);
  }

  getDateLabel(date: Date): string {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const tomorrow = new Date(today);
    tomorrow.setDate(tomorrow.getDate() + 1);

    if (date.getTime() === today.getTime()) return 'Hôm nay';
    if (date.getTime() === tomorrow.getTime()) return 'Ngày mai';
    return '';
  }

  formatShowTime(time: string | Date): string {
    if (!time) return '-';
    return dayjs(time, 'HH:mm:ss').format('HH:mm');
  }

  onSelect(showtimeId: number) {
    this.selectedShowtimeId.set(showtimeId);
  }

  confirmSelection() {
    if (this.selectedShowtimeId()) {
      this.modalRef.close(this.selectedShowtimeId());
    }
  }
}
