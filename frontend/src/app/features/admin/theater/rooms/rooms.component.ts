import {
  Component,
  computed,
  inject,
  input,
  numberAttribute,
  OnInit,
  signal,
  ChangeDetectionStrategy,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzSelectModule } from 'ng-zorro-antd/select';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzTagModule } from 'ng-zorro-antd/tag';
import { NzModalService } from 'ng-zorro-antd/modal';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { RoomService } from '@domain/theater/data-access/room.service';
import { SeatLayoutService } from '@domain/theater/data-access/seat-layout.service';
import { Room } from '@domain/theater/models/room.model';
import {
  SeatTemplate,
  SeatTemplateType,
} from '@domain/theater/models/seat-template.model';
import { LoadingComponent } from '@shared/components/loading/loading.component';
import { ErrorStateComponent } from '@shared/components/error-state/error-state.component';
import { confirmDelete } from '@shared/utils/confirm-delete';
import { errorMessage } from '@shared/utils/error.helper';

const TYPE_ORDER: SeatTemplateType[] = ['NORMAL', 'VIP', 'COUPLE', 'WHEELCHAIR'];

@Component({
  selector: 'app-rooms',
  imports: [
    FormsModule,
    NzButtonModule,
    NzInputModule,
    NzSelectModule,
    NzIconModule,
    NzTagModule,
    TranslatePipe,
    LoadingComponent,
    ErrorStateComponent,
  ],
  templateUrl: './rooms.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [NzModalService],
})
export class RoomsComponent implements OnInit {
  private roomService = inject(RoomService);
  private layoutService = inject(SeatLayoutService);
  private modalService = inject(NzModalService);
  private translate = inject(TranslateService);

  theaterId = input(0, { transform: numberAttribute });

  readonly rooms = signal<Room[]>([]);
  readonly selectedRoom = signal<Room | null>(null);
  readonly layout = signal<SeatTemplate[]>([]);
  readonly selectedIds = signal<Set<number>>(new Set());
  readonly dirty = signal(false);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);

  readonly showRoomForm = signal(false);
  readonly editingRoom = signal<Room | null>(null);
  readonly roomName = signal('');
  readonly roomType = signal('2D');
  readonly roomSeats = signal<number | null>(50);

  readonly grouped = computed(() => {
    const groups = new Map<string, SeatTemplate[]>();
    for (const s of this.layout()) {
      const list = groups.get(s.rowLabel) ?? [];
      list.push(s);
      groups.set(s.rowLabel, list);
    }
    return [...groups.entries()]
      .sort(([a], [b]) => a.localeCompare(b))
      .map(([row, seats]) => ({
        row,
        seats: [...seats].sort((a, b) => a.seatNumber - b.seatNumber),
      }));
  });

  readonly selectedCount = computed(() => this.selectedIds().size);

  ngOnInit() {
    this.loadRooms();
  }

  loadRooms() {
    const theaterId = this.theaterId();
    if (!theaterId) return;
    this.loading.set(true);
    this.roomService.getByTheater(theaterId).subscribe({
      next: (rooms) => {
        this.rooms.set(rooms);
        this.loading.set(false);
        if (rooms.length > 0 && !this.selectedRoom()) {
          this.selectRoom(rooms[0]);
        }
      },
      error: (err) => {
        this.error.set(errorMessage(err));
        this.loading.set(false);
      },
    });
  }

  openAdd() {
    this.editingRoom.set(null);
    this.roomName.set('');
    this.roomType.set('2D');
    this.roomSeats.set(50);
    this.showRoomForm.set(true);
  }

  openEdit(room: Room) {
    this.editingRoom.set(room);
    this.roomName.set(room.name);
    this.roomType.set(room.roomType);
    this.roomSeats.set(room.totalSeats);
    this.showRoomForm.set(true);
  }

  saveRoom() {
    const theaterId = this.theaterId();
    const name = this.roomName().trim();
    const totalSeats = this.roomSeats() ?? 0;
    if (!theaterId || !name) return;
    this.saving.set(true);
    const payload = { name, roomType: this.roomType(), totalSeats };
    const editing = this.editingRoom();
    const done = () => {
      this.saving.set(false);
      this.showRoomForm.set(false);
      this.loadRooms();
    };
    if (editing) {
      this.roomService.update(editing.id, payload).subscribe({
        next: done,
        error: (err) => {
          this.error.set(errorMessage(err));
          this.saving.set(false);
        },
      });
    } else {
      this.roomService.create(theaterId, payload).subscribe({
        next: done,
        error: (err) => {
          this.error.set(errorMessage(err));
          this.saving.set(false);
        },
      });
    }
  }

  deleteRoom(room: Room) {
    confirmDelete(
      this.modalService,
      this.translate,
      'admin.room.deleteTitle',
      () =>
        this.roomService.remove(room.id).subscribe({
          next: () => {
            if (this.selectedRoom()?.id === room.id) {
              this.selectedRoom.set(null);
              this.layout.set([]);
            }
            this.loadRooms();
          },
          error: (err) => this.error.set(errorMessage(err)),
        }),
    );
  }

  selectRoom(room: Room) {
    this.selectedRoom.set(room);
    this.selectedIds.set(new Set());
    this.dirty.set(false);
    this.loading.set(true);
    this.layoutService.getByRoom(room.id).subscribe({
      next: (layout) => {
        this.layout.set(layout);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(errorMessage(err));
        this.loading.set(false);
      },
    });
  }

  cycleType(seat: SeatTemplate) {
    const next =
      TYPE_ORDER[(TYPE_ORDER.indexOf(seat.seatType as SeatTemplateType) + 1) %
        TYPE_ORDER.length];
    this.layout.update((list) =>
      list.map((s) => (s.id === seat.id ? { ...s, seatType: next } : s)),
    );
    this.dirty.set(true);
  }

  toggleSelect(seat: SeatTemplate) {
    this.selectedIds.update((set) => {
      const next = new Set(set);
      if (next.has(seat.id)) next.delete(seat.id);
      else next.add(seat.id);
      return next;
    });
  }

  selectRow(rowLabel: string) {
    const ids = this.layout()
      .filter((s) => s.rowLabel === rowLabel)
      .map((s) => s.id);
    this.selectedIds.update((set) => {
      const next = new Set(set);
      const allIn = ids.every((id) => next.has(id));
      ids.forEach((id) => (allIn ? next.delete(id) : next.add(id)));
      return next;
    });
  }

  applyTypeToSelected(type: SeatTemplateType) {
    const ids = this.selectedIds();
    if (ids.size === 0) return;
    this.layout.update((list) =>
      list.map((s) => (ids.has(s.id) ? { ...s, seatType: type } : s)),
    );
    this.dirty.set(true);
  }

  addRow() {
    const rows = this.grouped().map((g) => g.row);
    const nextChar = rows.length
      ? String.fromCharCode(rows[rows.length - 1].charCodeAt(0) + 1)
      : 'A';
    const width = Math.max(
      8,
      ...this.grouped().map((g) => g.seats.length),
    );
    const roomId = this.selectedRoom()?.id ?? 0;
    const fresh: SeatTemplate[] = Array.from({ length: width }, (_, i) => ({
      id: -Date.now() - i,
      roomId,
      rowLabel: nextChar,
      seatNumber: i + 1,
      seatType: 'NORMAL',
      posX: i,
      posY: rows.length,
    }));
    this.layout.update((list) => [...list, ...fresh]);
    this.dirty.set(true);
  }

  saveLayout() {
    const room = this.selectedRoom();
    if (!room || !this.dirty()) return;
    this.saving.set(true);
    const payload = this.layout().map((s) => ({
      rowLabel: s.rowLabel,
      seatNumber: s.seatNumber,
      seatType: s.seatType,
      posX: s.posX,
      posY: s.posY,
    }));
    this.layoutService.replaceLayout(room.id, payload).subscribe({
      next: (layout) => {
        this.layout.set(layout);
        this.selectedIds.set(new Set());
        this.dirty.set(false);
        this.saving.set(false);
      },
      error: (err) => {
        this.error.set(errorMessage(err));
        this.saving.set(false);
      },
    });
  }

  seatClass(seatType: string): string {
    switch (seatType) {
      case 'VIP':
        return 'bg-[var(--color-gold)] text-white border-[var(--color-gold)]';
      case 'COUPLE':
        return 'bg-purple-500 text-white border-purple-500';
      case 'WHEELCHAIR':
        return 'bg-[var(--color-info)] text-white border-[var(--color-info)]';
      default:
        return 'bg-slate-200 text-slate-600 border-slate-300 dark:bg-slate-700 dark:text-slate-200';
    }
  }
}
