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
  templateUrl: './showtime-section.component.html',
  styleUrls: ['./showtime-section.component.css'],
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
