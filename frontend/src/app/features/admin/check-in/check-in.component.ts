import {
  Component,
  ChangeDetectionStrategy,
  inject,
  signal,
  viewChild,
  ElementRef,
  OnDestroy,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzTagModule } from 'ng-zorro-antd/tag';
import { NzResultModule } from 'ng-zorro-antd/result';
import { NzSpinModule } from 'ng-zorro-antd/spin';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { ToastService } from '@core/services/toast/toast.service';
import { ReservationService } from '@domain/reservation/data-access/reservation.service';
import { Reservation } from '@domain/reservation/models/reservation.model';
import { TicketStubComponent } from '@shared/components/ticket-stub/ticket-stub.component';

@Component({
  selector: 'app-check-in',
  imports: [
    CommonModule,
    FormsModule,
    NzButtonModule,
    NzInputModule,
    NzIconModule,
    NzTagModule,
    NzResultModule,
    NzSpinModule,
    TranslatePipe,
    TicketStubComponent,
  ],
  templateUrl: './check-in.component.html',
  styleUrls: ['./check-in.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CheckInComponent implements OnDestroy {
  private reservationService = inject(ReservationService);
  private translate = inject(TranslateService);
  private toast = inject(ToastService);

  readonly videoElement = viewChild<ElementRef<HTMLVideoElement>>('videoElement');

  code = '';
  loading = signal(false);
  error = signal<string | null>(null);
  reservation = signal<Reservation | null>(null);
  checkedIn = signal(false);
  cameraOpen = signal(false);
  cameraError = signal<string | null>(null);

  private mediaStream: MediaStream | null = null;
  private scanIntervalId: any = null;

  ngOnDestroy(): void {
    this.stopCamera();
  }

  toggleCamera() {
    const willOpen = !this.cameraOpen();
    this.cameraOpen.set(willOpen);
    if (willOpen) {
      setTimeout(() => this.startCamera(), 100);
    } else {
      this.stopCamera();
    }
  }

  async startCamera() {
    this.cameraError.set(null);
    try {
      if (!navigator.mediaDevices?.getUserMedia) {
        this.cameraError.set('Trình duyệt không hỗ trợ camera. Vui lòng nhập mã thủ công.');
        return;
      }
      this.mediaStream = await navigator.mediaDevices.getUserMedia({
        video: { facingMode: 'environment', width: { ideal: 640 }, height: { ideal: 480 } },
      });
      const video = this.videoElement()?.nativeElement;
      if (video) {
        video.srcObject = this.mediaStream;
        await video.play();
        this.startScanning(video);
      }
    } catch (err: any) {
      this.cameraError.set(err?.message || 'Không thể truy cập camera.');
    }
  }

  stopCamera() {
    if (this.scanIntervalId) {
      clearInterval(this.scanIntervalId);
      this.scanIntervalId = null;
    }
    if (this.mediaStream) {
      this.mediaStream.getTracks().forEach((track) => track.stop());
      this.mediaStream = null;
    }
    const video = this.videoElement()?.nativeElement;
    if (video) {
      video.srcObject = null;
    }
  }

  private playScanBeep(tone: 'scan' | 'success' | 'error' = 'scan') {
    try {
      const AudioContextClass = window.AudioContext || (window as any).webkitAudioContext;
      if (!AudioContextClass) return;
      const ctx = new AudioContextClass();

      if (tone === 'scan') {
        // High-pitched quick POS scanner chirp (1300Hz -> 1800Hz)
        const osc = ctx.createOscillator();
        const gain = ctx.createGain();
        osc.type = 'sine';
        osc.frequency.setValueAtTime(1300, ctx.currentTime);
        osc.frequency.exponentialRampToValueAtTime(1800, ctx.currentTime + 0.08);
        gain.gain.setValueAtTime(0.2, ctx.currentTime);
        gain.gain.exponentialRampToValueAtTime(0.01, ctx.currentTime + 0.08);
        osc.connect(gain);
        gain.connect(ctx.destination);
        osc.start();
        osc.stop(ctx.currentTime + 0.08);
      } else if (tone === 'success') {
        // 2-tone melodic success chime (880Hz -> 1320Hz)
        const now = ctx.currentTime;
        const osc1 = ctx.createOscillator();
        const gain1 = ctx.createGain();
        osc1.frequency.setValueAtTime(880, now);
        gain1.gain.setValueAtTime(0.25, now);
        gain1.gain.exponentialRampToValueAtTime(0.01, now + 0.1);
        osc1.connect(gain1);
        gain1.connect(ctx.destination);
        osc1.start(now);
        osc1.stop(now + 0.1);

        const osc2 = ctx.createOscillator();
        const gain2 = ctx.createGain();
        osc2.frequency.setValueAtTime(1320, now + 0.1);
        gain2.gain.setValueAtTime(0.25, now + 0.1);
        gain2.gain.exponentialRampToValueAtTime(0.01, now + 0.25);
        osc2.connect(gain2);
        gain2.connect(ctx.destination);
        osc2.start(now + 0.1);
        osc2.stop(now + 0.25);
      } else {
        // Low buzzer for invalid/error (300Hz)
        const osc = ctx.createOscillator();
        const gain = ctx.createGain();
        osc.type = 'sawtooth';
        osc.frequency.setValueAtTime(280, ctx.currentTime);
        gain.gain.setValueAtTime(0.2, ctx.currentTime);
        gain.gain.exponentialRampToValueAtTime(0.01, ctx.currentTime + 0.25);
        osc.connect(gain);
        gain.connect(ctx.destination);
        osc.start();
        osc.stop(ctx.currentTime + 0.25);
      }
    } catch {}
  }

  private startScanning(video: HTMLVideoElement) {
    if ('BarcodeDetector' in window) {
      const detector = new (window as any).BarcodeDetector({
        formats: ['qr_code', 'code_128', 'ean_13', 'code_39'],
      });
      this.scanIntervalId = setInterval(async () => {
        try {
          const barcodes = await detector.detect(video);
          if (barcodes.length > 0) {
            const detectedCode = barcodes[0].rawValue;
            if (detectedCode && detectedCode.trim()) {
              this.playScanBeep('scan');
              this.code = detectedCode.trim();
              this.stopCamera();
              this.cameraOpen.set(false);
              this.search();
            }
          }
        } catch {}
      }, 500);
    }
  }

  search() {
    const id = this.code.trim();
    if (!id) return;
    this.loading.set(true);
    this.error.set(null);
    this.reservationService.getReservation(id).subscribe({
      next: (res) => {
        this.reservation.set(res);
        this.checkedIn.set(res.checkedIn ?? res.reservationStatus === 'CONFIRMED');
        this.loading.set(false);
      },
      error: () => {
        this.playScanBeep('error');
        this.error.set(this.translate.instant('checkin.notFound'));
        this.loading.set(false);
      },
    });
  }

  confirm() {
    const res = this.reservation();
    if (!res || this.loading()) return;
    this.loading.set(true);
    this.error.set(null);
    this.reservationService.checkIn(res.reservationCode || this.code.trim()).subscribe({
      next: (updated) => {
        this.playScanBeep('success');
        this.reservation.set(updated);
        this.checkedIn.set(updated.checkedIn ?? true);
        this.loading.set(false);
        this.toast.createNotification({
          type: 'success',
          title: this.translate.instant('checkin.done'),
          message: this.translate.instant('checkin.doneSub'),
        });
      },
      error: (err) => {
        this.playScanBeep('error');
        this.error.set(
          typeof err?.message === 'string' && err.message
            ? err.message
            : this.translate.instant('checkin.notFound'),
        );
        this.loading.set(false);
      },
    });
  }

  isCheckedIn() {
    return this.checkedIn();
  }

  ticketMeta(res: Reservation) {
    const t = this.translate.instant.bind(this.translate);
    return [
      {
        label: t('checkin.theater'),
        value: `${res.showtime.theaterName} • ${res.showtime.theaterLocation}`,
      },
      {
        label: t('checkin.showtime'),
        value: `${new Date(res.showtime.showDate).toLocaleDateString('vi-VN')} ${new Date(res.showtime.showTime).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })}`,
        accent: true,
      },
      { label: t('checkin.customer'), value: res.user.username },
      {
        label: t('checkin.seats'),
        value: res.seats.join(', '),
      },
    ];
  }

  checkinWindowStatus(res: Reservation): { status: 'VALID' | 'EARLY' | 'LATE'; text: string; badgeClass: string } {
    try {
      const showDate = new Date(res.showtime.showDate);
      const showTime = new Date(res.showtime.showTime);
      const showDateTime = new Date(showDate.getFullYear(), showDate.getMonth(), showDate.getDate(), showTime.getHours(), showTime.getMinutes());
      const now = new Date();
      const openWindow = new Date(showDateTime.getTime() - 30 * 60 * 1000);
      const closeWindow = new Date(showDateTime.getTime() + 20 * 60 * 1000);

      if (now < openWindow) {
        return {
          status: 'EARLY',
          text: 'Chưa tới giờ vào cửa (Mở trước giờ chiếu 30p)',
          badgeClass: 'text-amber-500 bg-amber-500/10 border-amber-500/30',
        };
      }
      if (now > closeWindow) {
        return {
          status: 'LATE',
          text: 'Đã quá thời gian vào cửa (Quá 20p sau giờ chiếu)',
          badgeClass: 'text-red-500 bg-red-500/10 border-red-500/30',
        };
      }
      return {
        status: 'VALID',
        text: 'Khung giờ vào cửa hợp lệ (-30p đến +20p)',
        badgeClass: 'text-emerald-500 bg-emerald-500/10 border-emerald-500/30',
      };
    } catch {
      return {
        status: 'VALID',
        text: 'Khung giờ vào cửa hợp lệ',
        badgeClass: 'text-emerald-500 bg-emerald-500/10 border-emerald-500/30',
      };
    }
  }
}
