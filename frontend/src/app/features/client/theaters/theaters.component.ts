import { Component, inject, ChangeDetectionStrategy } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
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

  // Keyed by theaterId — cancels the previous theater's in-flight request.
  private readonly roomsResource = rxResource({
    params: () => this.theaterId() ?? undefined,
    stream: ({ params: id }) => this.roomService.getByTheater(id),
    defaultValue: [] as Room[],
  });

  readonly rooms = this.roomsResource.value;
  readonly roomsError = this.roomsResource.error;

  reloadRooms(): void {
    this.roomsResource.reload();
  }
}
