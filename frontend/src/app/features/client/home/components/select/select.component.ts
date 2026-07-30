import { Component, inject } from '@angular/core';
import { Store } from '@ngrx/store';
import { NzSelectModule } from 'ng-zorro-antd/select';
import { AsyncPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NzIconDirective } from 'ng-zorro-antd/icon';
import { NzDropDownModule } from 'ng-zorro-antd/dropdown';
import { TranslatePipe } from '@ngx-translate/core';
import { combineLatest, map } from 'rxjs';
import {
  selectAllTheaters,
  selectSelectedTheaterId,
} from '@domain/theater/data-access/theater.selectors';
import { TheaterActions } from '@domain/theater/data-access/theater.actions';
import { ShowtimeActions } from '@domain/showtime/data-access/showtime.actions';
import { StorageService } from '@core/services/storage/storage.service';
import { Theater } from '@domain/theater/models/theater.model';

@Component({
  selector: 'app-theater-select',
  imports: [
    NzSelectModule,
    AsyncPipe,
    FormsModule,
    NzIconDirective,
    NzDropDownModule,
    TranslatePipe,
  ],
  templateUrl: './select.component.html',
})
export class SelectShowtimeComponent {
  private store = inject(Store);
  private storage = inject(StorageService);

  theaters$ = this.store.select(selectAllTheaters);
  selectedTheaterId$ = this.store.select(selectSelectedTheaterId);

  selectTheaterName$ = combineLatest([
    this.theaters$,
    this.selectedTheaterId$,
  ]).pipe(
    map(([theaters, id]) => {
      const found = theaters.find((t: Theater) => t.id === id);
      return found ? found.name : 'Chọn rạp chiếu';
    }),
  );

  onSelect(theaterId: number) {
    this.store.dispatch(TheaterActions.setSelectedTheaterId({ theaterId }));
    this.storage.setSelectedTheaterId(theaterId);
    this.store.dispatch(ShowtimeActions.loadShowtimesByTheaterId({ theaterId, page: 0, size: 100 }));
  }
}
