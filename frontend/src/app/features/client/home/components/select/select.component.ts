import {
  Component,
  computed,
  inject,
  ChangeDetectionStrategy,
} from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { Store } from '@ngrx/store';
import { FormsModule } from '@angular/forms';
import { NzIconDirective } from 'ng-zorro-antd/icon';
import { NzDropDownModule } from 'ng-zorro-antd/dropdown';
import { TranslateService } from '@ngx-translate/core';
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
  imports: [FormsModule, NzIconDirective, NzDropDownModule],
  templateUrl: './select.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SelectShowtimeComponent {
  private store = inject(Store);
  private storage = inject(StorageService);
  private translate = inject(TranslateService);

  private theaters = toSignal(this.store.select(selectAllTheaters), {
    initialValue: [],
  });
  private selectedTheaterId = toSignal(
    this.store.select(selectSelectedTheaterId),
    { initialValue: 0 },
  );

  readonly theaterList = this.theaters;
  readonly currentTheaterId = this.selectedTheaterId;
  readonly selectTheaterName = computed(() => {
    const found = this.theaters().find(
      (t: Theater) => t.id === this.selectedTheaterId(),
    );
    return found ? found.name : this.translate.instant('home.selectTheater');
  });

  onSelect(theaterId: number) {
    this.store.dispatch(TheaterActions.setSelectedTheaterId({ theaterId }));
    this.storage.setSelectedTheaterId(theaterId);
    this.store.dispatch(
      ShowtimeActions.loadShowtimesByTheaterId({
        theaterId,
        page: 0,
        size: 100,
      }),
    );
  }
}
