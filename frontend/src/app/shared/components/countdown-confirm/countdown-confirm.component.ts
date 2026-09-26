import {
  Component,
  OnDestroy,
  OnInit,
  inject,
  signal,
  ChangeDetectionStrategy,
} from '@angular/core';
import { NZ_MODAL_DATA, NzModalRef } from 'ng-zorro-antd/modal';
import { NzIconModule } from 'ng-zorro-antd/icon';

export interface CountdownConfirmData {
  message: string;
  seconds: number;
  autoCancelLabel: string;
}

// Destructive confirm that auto-cancels when the countdown hits zero.
// Esc / backdrop are disabled by the caller; closing always means cancel.
@Component({
  selector: 'app-countdown-confirm',
  imports: [NzIconModule],
  templateUrl: './countdown-confirm.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CountdownConfirmComponent implements OnInit, OnDestroy {
  private modalRef = inject(NzModalRef);
  private data = inject(NZ_MODAL_DATA) as CountdownConfirmData;

  readonly message = signal('');
  readonly autoCancelLabel = signal('');
  private total = 20;

  readonly remaining = signal(0);
  private timer: ReturnType<typeof setInterval> | null = null;

  ngOnInit() {
    this.message.set(this.data.message ?? '');
    this.autoCancelLabel.set(this.data.autoCancelLabel ?? '');
    this.total = this.data.seconds && this.data.seconds > 0 ? this.data.seconds : 20;
    this.remaining.set(this.total);
    this.timer = setInterval(() => {
      const next = this.remaining() - 1;
      if (next <= 0) {
        this.modalRef.destroy({ confirmed: false, timedOut: true });
        return;
      }
      this.remaining.set(next);
    }, 1000);
  }

  ngOnDestroy() {
    if (this.timer) clearInterval(this.timer);
  }
}
