import {
  Component,
  ChangeDetectionStrategy,
  inject,
  signal,
  computed,
  OnInit,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzSelectModule } from 'ng-zorro-antd/select';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzTagModule } from 'ng-zorro-antd/tag';
import { NzSpinModule } from 'ng-zorro-antd/spin';
import { NzModalModule } from 'ng-zorro-antd/modal';
import { NzRadioModule } from 'ng-zorro-antd/radio';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { ToastService } from '@core/services/toast/toast.service';
import { TheaterService } from '@domain/theater/data-access/theater.service';
import { ShowtimeService } from '@domain/showtime/data-access/showtime.service';
import { SeatService } from '@domain/seat/data-access/seat.service';
import { ReservationService, CounterSaleRequest } from '@domain/reservation/data-access/reservation.service';
import { Theater } from '@domain/theater/models/theater.model';
import { Showtime } from '@domain/showtime/models/showtime.model';
import { Seat } from '@domain/seat/models/seat.model';
import { Reservation } from '@domain/reservation/models/reservation.model';
import { SeatIconComponent } from '@shared/components/seat/seat.component';
import { BarcodeComponent } from '@shared/components/barcode/barcode.component';
import { buildFixedRow, trimOuterEmptyCells } from '@shared/utils/seat-layout';

@Component({
  selector: 'app-counter-sale',
  imports: [
    CommonModule,
    FormsModule,
    NzButtonModule,
    NzSelectModule,
    NzInputModule,
    NzIconModule,
    NzTagModule,
    NzSpinModule,
    NzModalModule,
    NzRadioModule,
    SeatIconComponent,
    BarcodeComponent,
  ],
  templateUrl: './counter-sale.component.html',
  styleUrls: ['./counter-sale.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CounterSaleComponent implements OnInit {
  private theaterService = inject(TheaterService);
  private showtimeService = inject(ShowtimeService);
  private seatService = inject(SeatService);
  private reservationService = inject(ReservationService);
  private toast = inject(ToastService);
  private translate = inject(TranslateService);

  // States
  theaters = signal<Theater[]>([]);
  selectedTheaterId = signal<number | null>(null);
  showtimes = signal<Showtime[]>([]);
  selectedShowtime = signal<Showtime | null>(null);

  loadingShowtimes = signal(false);
  loadingSeats = signal(false);
  submitting = signal(false);

  // Seats data
  seats = signal<Seat[]>([]);
  selectedSeats = signal<Seat[]>([]);

  // Checkout form
  customerName = 'Khách mua tại quầy';
  customerPhone = '';
  paymentMethod = 'CASH'; // CASH, CARD_POS, TRANSFER
  cashGiven = 0;

  // Completed reservation result modal
  completedReservation = signal<Reservation | null>(null);
  showSuccessModal = signal(false);

  // Dates selection (today, tomorrow, next 5 days)
  availableDates: { label: string; date: string }[] = [];
  selectedDate = signal<string>('');

  ngOnInit(): void {
    this.initDates();
    this.loadTheaters();
  }

  private initDates(): void {
    const dates: { label: string; date: string }[] = [];
    const today = new Date();
    for (let i = 0; i < 5; i++) {
      const d = new Date(today);
      d.setDate(today.getDate() + i);
      const iso = d.toISOString().split('T')[0];
      let label = `${d.getDate()}/${d.getMonth() + 1}`;
      if (i === 0) label = `Hôm nay (${label})`;
      else if (i === 1) label = `Ngày mai (${label})`;
      else {
        const dayNames = ['CN', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7'];
        label = `${dayNames[d.getDay()]} (${label})`;
      }
      dates.push({ label, date: iso });
    }
    this.availableDates = dates;
    this.selectedDate.set(dates[0].date);
  }

  loadTheaters(): void {
    this.theaterService.getTheaters().subscribe({
      next: (res) => {
        const list = res.content || [];
        this.theaters.set(list);
        if (list.length > 0) {
          this.onTheaterChange(list[0].id);
        }
      },
      error: () => {
        this.toast.createNotification({
          type: 'error',
          title: 'Lỗi nạp danh sách rạp',
          message: 'Không thể kết nối máy chủ để lấy dữ liệu cụm rạp.',
        });
      },
    });
  }

  onTheaterChange(theaterId: number): void {
    this.selectedTheaterId.set(theaterId);
    this.selectedShowtime.set(null);
    this.seats.set([]);
    this.selectedSeats.set([]);
    this.loadShowtimes();
  }

  onDateChange(date: string): void {
    this.selectedDate.set(date);
    this.selectedShowtime.set(null);
    this.seats.set([]);
    this.selectedSeats.set([]);
    this.loadShowtimes();
  }

  loadShowtimes(): void {
    const theaterId = this.selectedTheaterId();
    if (!theaterId) return;

    this.loadingShowtimes.set(true);
    this.showtimeService.getShowtimeByTheater(theaterId).subscribe({
      next: (res) => {
        const all = res.content || [];
        const dateStr = this.selectedDate();
        // Filter by selected date
        const filtered = all.filter((st) => {
          if (!st.showDate) return false;
          const stDate = new Date(st.showDate).toISOString().split('T')[0];
          return stDate === dateStr;
        });
        this.showtimes.set(filtered);
        this.loadingShowtimes.set(false);
      },
      error: () => {
        this.loadingShowtimes.set(false);
      },
    });
  }

  selectShowtime(st: Showtime): void {
    this.selectedShowtime.set(st);
    this.selectedSeats.set([]);
    this.loadSeats(st.id);
  }

  loadSeats(showtimeId: number): void {
    this.loadingSeats.set(true);
    this.seatService.getSeatByShowtime(showtimeId).subscribe({
      next: (seats) => {
        this.seats.set(seats);
        this.loadingSeats.set(false);
      },
      error: () => {
        this.loadingSeats.set(false);
        this.toast.createNotification({
          type: 'error',
          title: 'Lỗi sơ đồ ghế',
          message: 'Không thể tải trạng thái ghế của suất chiếu này.',
        });
      },
    });
  }

  // Grouped seats for layout rendering
  groupedSeats = computed(() => {
    const seats = this.seats();
    const groups = seats.reduce(
      (acc, seat) => {
        const row = seat.seatNumber.charAt(0);
        acc[row] = acc[row] || [];
        acc[row].push(seat);
        return acc;
      },
      {} as Record<string, Seat[]>,
    );

    const sortRowLabels = (a: string, b: string) =>
      a.length === b.length ? a.localeCompare(b) : a.length - b.length;

    return Object.keys(groups)
      .sort(sortRowLabels)
      .map((row) => ({
        row,
        cells: trimOuterEmptyCells(
          buildFixedRow(
            groups[row].map((s) => ({
              seatNumber: parseInt(s.seatNumber.slice(1)),
              seatType: s.seatType ?? 'NORMAL',
              ref: s,
            })),
          ),
        ).map((cell) => ({
          ...cell,
          seats: cell.seats.map((c) => c.ref),
        })),
      }));
  });

  toggleSeat(seat: Seat): void {
    if (seat.status === 'BOOKED' || seat.status === 'HOLD') {
      return;
    }

    const currentSelected = this.selectedSeats();
    const isAlreadySelected = currentSelected.some((s) => s.id === seat.id);

    if (isAlreadySelected) {
      this.selectedSeats.set(currentSelected.filter((s) => s.id !== seat.id));
    } else {
      if (currentSelected.length >= 8) {
        this.toast.createNotification({
          type: 'warning',
          title: 'Giới hạn số lượng',
          message: 'Mỗi lượt xuất vé tối đa 8 ghế.',
        });
        return;
      }
      this.selectedSeats.set([...currentSelected, seat]);
    }
  }

  isSeatSelected(seatId: number): boolean {
    return this.selectedSeats().some((s) => s.id === seatId);
  }

  getSeatClass(seat: Seat): string {
    const base = 'transition-all duration-200 ease-in-out cursor-pointer';
    if (seat.status === 'BOOKED') {
      return 'text-danger/60 cursor-not-allowed opacity-50';
    }
    if (seat.status === 'HOLD') {
      return 'text-warning/80 cursor-not-allowed opacity-75';
    }
    if (this.isSeatSelected(seat.id)) {
      return 'text-primary scale-110 drop-shadow-[0_0_8px_rgba(229,9,20,0.6)]';
    }
    return `${base} text-muted hover:text-success hover:scale-105`;
  }

  iconVariant(cell: { span: number }, seat: Seat): 'single' | 'couple' | 'vip' {
    if (cell.span > 1) return 'couple';
    return seat.seatType === 'VIP' ? 'vip' : 'single';
  }

  removeSeat(seatId: number): void {
    this.selectedSeats.set(this.selectedSeats().filter((s) => s.id !== seatId));
  }

  clearSelection(): void {
    this.selectedSeats.set([]);
  }

  // Price calculations
  totalPrice = computed(() => {
    const st = this.selectedShowtime();
    if (!st) return 0;
    const basePrice = st.price || 85000;
    return this.selectedSeats().reduce((sum, seat) => {
      let multiplier = 1;
      if (seat.seatType === 'VIP') multiplier = 1.2;
      else if (seat.seatType === 'DOUBLE') multiplier = 2;
      return sum + Math.round(basePrice * multiplier);
    }, 0);
  });

  // Calculate change for cash payment
  changeDue = computed(() => {
    if (this.paymentMethod !== 'CASH') return 0;
    const total = this.totalPrice();
    const given = this.cashGiven || 0;
    return Math.max(0, given - total);
  });

  setQuickCash(amount: number | 'exact'): void {
    if (amount === 'exact') {
      this.cashGiven = this.totalPrice();
    } else {
      this.cashGiven = amount;
    }
  }

  // Group showtimes by movie
  movieGroups = computed(() => {
    const sts = this.showtimes();
    const map = new Map<number, { title: string; poster: string; showtimes: Showtime[] }>();
    for (const st of sts) {
      const movieId = st.movieId;
      if (!map.has(movieId)) {
        map.set(movieId, {
          title: st.movieTitle || 'Phim ' + movieId,
          poster: (st as any).moviePoster || '',
          showtimes: [],
        });
      }
      map.get(movieId)!.showtimes.push(st);
    }
    return Array.from(map.values());
  });

  formatCurrency(val: number): string {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val);
  }

  formatTime(isoDate: string | Date | undefined): string {
    if (!isoDate) return '--:--';
    const d = new Date(isoDate);
    return d.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
  }

  formatDate(isoDate: string | Date | undefined): string {
    if (!isoDate) return '--/--/----';
    const d = new Date(isoDate);
    return d.toLocaleDateString('vi-VN');
  }

  // Submit Counter Sale
  submitCounterSale(): void {
    const st = this.selectedShowtime();
    const selected = this.selectedSeats();

    if (!st || selected.length === 0) {
      this.toast.createNotification({
        type: 'warning',
        title: 'Chưa đủ thông tin',
        message: 'Vui lòng chọn suất chiếu và ít nhất một ghế.',
      });
      return;
    }

    if (this.paymentMethod === 'CASH' && this.cashGiven > 0 && this.cashGiven < this.totalPrice()) {
      this.toast.createNotification({
        type: 'warning',
        title: 'Tiền khách đưa chưa đủ',
        message: 'Số tiền khách đưa nhỏ hơn tổng giá trị vé.',
      });
      return;
    }

    const payload: CounterSaleRequest = {
      showtimeId: st.id,
      seatIds: selected.map((s) => s.id),
      paymentMethod: this.paymentMethod,
      customerName: this.customerName.trim() || 'Khách mua tại quầy',
      customerPhone: this.customerPhone.trim() || undefined,
    };

    this.submitting.set(true);
    this.reservationService.counterSale(payload).subscribe({
      next: (reservation) => {
        this.submitting.set(false);
        this.completedReservation.set(reservation);
        this.showSuccessModal.set(true);
        this.toast.createNotification({
          type: 'success',
          title: 'Xuất vé thành công!',
          message: `Mã vé: ${reservation.reservationCode}`,
        });
        // Reload seat status to reflect newly booked seats
        this.loadSeats(st.id);
        this.selectedSeats.set([]);
      },
      error: (err) => {
        this.submitting.set(false);
        this.toast.createNotification({
          type: 'error',
          title: 'Xuất vé thất bại',
          message: err?.message || 'Ghế đã bị đặt hoặc hệ thống gặp trục trặc.',
        });
      },
    });
  }

  printThermalTicket(): void {
    window.print();
  }

  startNewSale(): void {
    this.showSuccessModal.set(false);
    this.completedReservation.set(null);
    this.selectedSeats.set([]);
    this.cashGiven = 0;
  }
}
