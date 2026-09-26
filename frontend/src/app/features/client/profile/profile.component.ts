import {
  Component,
  inject,
  OnInit,
  signal,
  ChangeDetectionStrategy,
} from '@angular/core';
import { CommonModule, DOCUMENT } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzTabPosition, NzTabsModule } from 'ng-zorro-antd/tabs';
import { NzAvatarModule } from 'ng-zorro-antd/avatar';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzDropDownModule } from 'ng-zorro-antd/dropdown';
import { NzUploadModule } from 'ng-zorro-antd/upload';
import { NzTagModule } from 'ng-zorro-antd/tag';
import { NzBadgeModule } from 'ng-zorro-antd/badge';
import { NzModalModule } from 'ng-zorro-antd/modal';
import { NzQRCodeModule } from 'ng-zorro-antd/qr-code';
import { NzEmptyModule } from 'ng-zorro-antd/empty';
import { NzSkeletonModule } from 'ng-zorro-antd/skeleton';
import { NzTooltipModule } from 'ng-zorro-antd/tooltip';
import { BookingHistoryComponent } from '@features/client/profile/booking-history/booking-history.component';
import { InfoComponent } from '@features/client/profile/info/info.component';
import { UserVouchersComponent } from '@features/client/profile/user-vouchers/user-vouchers.component';
import { UserStore } from '@domain/user/data-access/user.store';
import { Store } from '@ngrx/store';
import { selectUserId } from '@core/auth/auth.selectors';
import { ReservationStore } from '@domain/reservation/data-access/reservation.store';
import { ReservationStatus } from '@core/constants/enum';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'app-profile',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    NzButtonModule,
    NzInputModule,
    NzFormModule,
    NzTabsModule,
    NzAvatarModule,
    NzIconModule,
    NzDropDownModule,
    NzUploadModule,
    NzTagModule,
    NzBadgeModule,
    NzTabsModule,
    NzUploadModule,
    NzModalModule,
    NzQRCodeModule,
    NzEmptyModule,
    NzSkeletonModule,
    NzTooltipModule,
    BookingHistoryComponent,
    InfoComponent,
    UserVouchersComponent,
    TranslatePipe,
  ],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { '(window:resize)': 'onResize()' },
})
export class ProfileComponent implements OnInit {
  private store = inject(Store);
  readonly userStore = inject(UserStore);
  readonly reservationStore = inject(ReservationStore);
  private document = inject(DOCUMENT);
  tabPosition: NzTabPosition = 'left';
  readonly ticketStatus = signal<ReservationStatus>('CONFIRMED');

  ngOnInit() {
    this.checkScreenSize();
    const userId = this.store.selectSignal(selectUserId)();

    if (userId !== undefined && userId !== null) {
      this.userStore.loadUser(userId);
      this.loadTickets();
    }
  }

  loadTickets() {
    const userId = this.store.selectSignal(selectUserId)();
    if (userId !== undefined && userId !== null) {
      this.reservationStore.loadReservationProfile({
        id: userId,
        status: this.ticketStatus(),
      });
    }
  }

  onTicketStatus(status: ReservationStatus) {
    this.ticketStatus.set(status);
    this.loadTickets();
  }

  onResize() {
    this.checkScreenSize();
  }

  checkScreenSize() {
    this.tabPosition =
      (this.document.defaultView?.innerWidth ?? 768) < 768 ? 'top' : 'left';
  }
}
