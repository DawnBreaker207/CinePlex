import {
  Component,
  effect,
  inject,
  signal,
  ChangeDetectionStrategy,
} from '@angular/core';
import { NzEmptyModule } from 'ng-zorro-antd/empty';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzTagModule } from 'ng-zorro-antd/tag';
import { TranslatePipe } from '@ngx-translate/core';
import { Store } from '@ngrx/store';
import {
  selectSelectedTheater,
  selectSelectedTheaterId,
} from '@domain/theater/data-access/theater.selectors';
import { RoomService } from '@domain/theater/data-access/room.service';
import { Room } from '@domain/theater/models/room.model';
import { ShowtimesComponent } from '@features/client/showtimes/showtimes.component';

@Component({
  selector: 'app-theaters',
  imports: [
    NzEmptyModule,
    NzButtonModule,
    NzIconModule,
    NzTagModule,
    TranslatePipe,
    ShowtimesComponent,
  ],
  templateUrl: './theaters.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TheatersComponent {
  private store = inject(Store);
  private roomService = inject(RoomService);

  readonly theater = this.store.selectSignal(selectSelectedTheater);
  readonly theaterId = this.store.selectSignal(selectSelectedTheaterId);
  readonly rooms = signal<Room[]>([]);

  constructor() {
    effect(() => {
      const id = this.theaterId();
      if (id) {
        this.roomService.getByTheater(id).subscribe({
          next: (rooms) => this.rooms.set(rooms),
          error: () => this.rooms.set([]),
        });
      } else {
        this.rooms.set([]);
      }
    });
  }
}
