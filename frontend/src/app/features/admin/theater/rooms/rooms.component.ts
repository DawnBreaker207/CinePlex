import {
  Component,
  computed,
  ElementRef,
  inject,
  input,
  numberAttribute,
  OnInit,
  signal,
  ViewChild,
  ChangeDetectionStrategy,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzSelectModule } from 'ng-zorro-antd/select';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzTagModule } from 'ng-zorro-antd/tag';
import { NzToolTipModule } from 'ng-zorro-antd/tooltip';
import { NzDrawerModule } from 'ng-zorro-antd/drawer';
import { NzModalService } from 'ng-zorro-antd/modal';
import { NzModalRef } from 'ng-zorro-antd/modal';
import { NzDrawerService } from 'ng-zorro-antd/drawer';
import { TypeSheetComponent } from './type-sheet/type-sheet.component';
import { SeatGridComponent } from './seat-grid/seat-grid.component';
import { SelectionPillComponent } from './selection-pill/selection-pill.component';
import { TourGuideComponent } from './tour-guide/tour-guide.component';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { RoomService } from '@domain/theater/data-access/room.service';
import { SeatLayoutService } from '@domain/theater/data-access/seat-layout.service';
import { Room } from '@domain/theater/models/room.model';
import {
  SeatTemplate,
  SeatTemplateType,
  SEAT_TYPES,
} from '@domain/theater/models/seat-template.model';
import {
  buildFixedRow,
  canMerge,
  FIXED_COLUMNS,
  seatTypeClass,
} from '@shared/utils/seat-layout';
import { LoadingComponent } from '@shared/components/loading/loading.component';
import { ErrorStateComponent } from '@shared/components/error-state/error-state.component';
import { confirmDelete } from '@shared/utils/confirm-delete';
import { errorMessage } from '@shared/utils/error.helper';
import { ToastService } from '@core/services/toast/toast.service';
import { CountdownConfirmComponent } from '@shared/components/countdown-confirm/countdown-confirm.component';
import { RoomsTourService } from './rooms-tour.service';
import { SeatHoldGestureService } from './seat-hold-gesture.service';

@Component({
  selector: 'app-rooms',
  imports: [
    FormsModule,
    NzButtonModule,
    NzInputModule,
    NzSelectModule,
    NzIconModule,
    NzTagModule,
    NzToolTipModule,
    NzDrawerModule,
    TranslatePipe,
    LoadingComponent,
    ErrorStateComponent,
    SeatGridComponent,
    SelectionPillComponent,
    TourGuideComponent,
  ],
  templateUrl: './rooms.component.html',
  styleUrl: './rooms.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [NzModalService, RoomsTourService, SeatHoldGestureService],
  host: {
    '(window:beforeunload)': 'onBeforeUnload($event)',
    '(document:keydown.escape)': 'onEscape()',
  },
})
export class RoomsComponent implements OnInit {
  private roomService = inject(RoomService);
  private layoutService = inject(SeatLayoutService);
  private modalService = inject(NzModalService);
  private drawerService = inject(NzDrawerService);
  private translate = inject(TranslateService);
  private toast = inject(ToastService);
  readonly tour = inject(RoomsTourService);
  readonly gesture = inject(SeatHoldGestureService);

  onBeforeUnload(event: BeforeUnloadEvent) {
    if (this.dirty()) event.preventDefault();
  }

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
  readonly editing = signal(false);
  // Touch devices have no hover: corner × always shows, tooltips tap-to-reveal.
  readonly isCoarsePointer = signal(
    typeof window !== 'undefined' &&
      typeof window.matchMedia === 'function' &&
      window.matchMedia('(pointer: coarse)').matches,
  );
  // Slots that had a seat deleted this session → show ghost number.
  readonly deletedGhosts = signal<Set<string>>(new Set());
  readonly totalSeats = computed(() => this.layout().length);
  readonly selectedCount = computed(() => this.selectedIds().size);
  // Floating pill state.
  readonly pillPos = signal<{ top: number; left: number; below: boolean } | null>(null);
  readonly pillHiddenByScroll = signal(false);
  readonly typeListOpen = signal(false);
  readonly lastSelectedId = signal<number | null>(null);
  private scrollTimer: ReturnType<typeof setTimeout> | null = null;

  @ViewChild('layoutPanel') layoutPanel?: ElementRef<HTMLElement>;
  @ViewChild('seatScroll') seatScroll?: ElementRef<HTMLElement>;
  readonly roomName = signal('');
  readonly roomType = signal('2D');
  readonly copySourceId = signal<number | null>(null);
  // Seat waiting for a double-tap partner to merge into a couple.
  readonly pendingMerge = signal<SeatTemplate | null>(null);
  private lastTap: { id: number; time: number } | null = null;

  onEscape() {
    this.disarm();
    this.clearSelection();
  }

  // Tapping the panel background (not a seat, cell, or control) exits selection.
  onPanelClick(event: MouseEvent) {
    const el = event.target as HTMLElement | null;
    if (el?.closest('button, nz-select, input, a, [role="button"]')) return;
    this.clearSelection();
  }

  deleteManyTitle(): string {
    return this.translate.instant('admin.room.deleteManyTitle', {
      n: this.selectedCount(),
    });
  }

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
        seats,
        cells: buildFixedRow(seats),
      }));
  });

  readonly typeCounts = computed(() => {
    const counts = new Map<SeatTemplateType, number>(
      SEAT_TYPES.map((t) => [t, 0]),
    );    for (const s of this.layout()) {
      const t = s.seatType as SeatTemplateType;
      if (counts.has(t)) counts.set(t, (counts.get(t) ?? 0) + 1);
    }
    return SEAT_TYPES.map((type) => ({ type, count: counts.get(type) ?? 0 }));
  });

  ngOnInit() {
    this.loadRooms();
  }

  loadRooms(selectId?: number) {
    const theaterId = this.theaterId();
    if (!theaterId) return;
    this.loading.set(true);
    this.roomService.getByTheater(theaterId).subscribe({
      next: (rooms) => {
        this.rooms.set(rooms);
        this.loading.set(false);
        const target = selectId
          ? rooms.find((r) => r.id === selectId)
          : !this.selectedRoom()
            ? rooms[0]
            : undefined;
        if (target) this.doSelectRoom(target);
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
    this.copySourceId.set(null);
    this.showRoomForm.set(true);
  }

  openEdit(room: Room) {
    this.editingRoom.set(room);
    this.roomName.set(room.name);
    this.roomType.set(room.roomType);
    this.copySourceId.set(null);
    this.showRoomForm.set(true);
  }

  saveRoom() {
    const theaterId = this.theaterId();
    const name = this.roomName().trim();
    if (!theaterId || !name) return;
    this.saving.set(true);
    const editing = this.editingRoom();
    if (editing) {
      // Total seats always sync from the real layout — never typed by hand.
      const payload = { name, roomType: this.roomType(), totalSeats: editing.totalSeats };
      this.roomService.update(editing.id, payload).subscribe({
        next: () => {
          this.saving.set(false);
          this.showRoomForm.set(false);
          this.loadRooms();
        },
        error: (err) => {
          this.error.set(errorMessage(err));
          this.saving.set(false);
        },
      });
      return;
    }
    const sourceId = this.copySourceId();
    // Optimistic: show the card + open edit mode immediately with a temp id.
    const tempId = -Date.now();
    const tempRoom: Room = {
      id: tempId,
      theaterId,
      name,
      roomType: this.roomType(),
      totalSeats: 0,
      seatCount: 0,
    };
    this.showRoomForm.set(false);
    this.rooms.update((list) => [...list, tempRoom]);
    this.selectedRoom.set(tempRoom);
    this.selectedIds.set(new Set());
    this.deletedGhosts.set(new Set());
    this.dirty.set(false);
    this.layout.set([]);
    this.editing.set(true);
    this.loading.set(false);
    this.roomService
      .create(theaterId, { name, roomType: this.roomType(), totalSeats: 0 })
      .subscribe({
        next: (created) => {
          this.saving.set(false);
          if (!created) {
            this.rooms.update((list) => list.filter((r) => r.id !== tempId));
            this.loadRooms();
            return;
          }
          // Swap the temp card for the real one (same position).
          this.rooms.update((list) =>
            list.map((r) => (r.id === tempId ? created : r)),
          );
          this.selectedRoom.set(created);
          if (sourceId) {
            this.loading.set(true);
            this.layoutService.getByRoom(sourceId).subscribe({
              next: (source) => {
                this.layout.set(
                  source.map((s, i) => ({
                    ...s,
                    id: -Date.now() - i,
                    roomId: created.id,
                  })),
                );
                this.dirty.set(true);
                this.loading.set(false);
              },
              error: (err) => {
                this.error.set(errorMessage(err));
                this.loading.set(false);
              },
            });
          }
        },
        error: (err) => {
          this.rooms.update((list) => list.filter((r) => r.id !== tempId));
          if (this.selectedRoom()?.id === tempId) {
            this.selectedRoom.set(null);
            this.layout.set([]);
            this.editing.set(false);
          }
          this.toast.createNotification({
            type: 'error',
            title: this.translate.instant('admin.room.createFailedTitle'),
            message: errorMessage(err),
            position: 'top',
          });
          this.saving.set(false);
        },
      });
  }

  deleteRoom(room: Room) {
    const modal: NzModalRef = this.modalService.create({
      nzTitle: this.translate.instant('admin.room.deleteTitle'),
      nzContent: CountdownConfirmComponent,
      nzData: {
        seconds: 20,
        message: this.translate.instant('admin.room.deleteRoomCountdown', {
          name: room.name,
        }),
        autoCancelLabel: this.translate.instant('admin.room.autoCancel'),
      },
      nzFooter: [
        {
          label: this.translate.instant('common.cancel'),
          onClick: () => modal.destroy(),
        },
        {
          label: this.translate.instant('common.delete'),
          type: 'primary',
          danger: true,
          onClick: () => {
            modal.destroy();
            this.deleteRoomOptimistic(room);
          },
        },
      ],
      nzClosable: false,
      nzMaskClosable: false,
    });
  }

  private deleteRoomOptimistic(room: Room) {
    const list = this.rooms();
    const idx = list.findIndex((r) => r.id === room.id);
    const wasSelected = this.selectedRoom()?.id === room.id;
    const layoutBackup = this.layout();
    // Optimistic: pull the card immediately, roll back on failure.
    this.rooms.update((l) => l.filter((r) => r.id !== room.id));
    if (wasSelected) {
      this.selectedRoom.set(null);
      this.layout.set([]);
      this.selectedIds.set(new Set());
      this.deletedGhosts.set(new Set());
      this.dirty.set(false);
      this.editing.set(false);
      this.disarm();
    }
    this.roomService.remove(room.id).subscribe({
      error: (err) => {
        this.rooms.update((l) => {
          const next = [...l];
          next.splice(Math.min(idx, next.length), 0, room);
          return next;
        });
        if (wasSelected) {
          this.selectedRoom.set(room);
          this.layout.set(layoutBackup);
        }
        this.toast.createNotification({
          type: 'error',
          title: this.translate.instant('admin.room.deleteFailedTitle'),
          message: errorMessage(err),
          position: 'top',
        });
      },
    });
  }

  // Stay-safe confirm: primary action keeps editing, leaving discards.
  private confirmDiscard(onLeave: () => void) {
    this.modalService.confirm({
      nzTitle: this.translate.instant('admin.room.discardTitle'),
      nzContent: this.translate.instant('admin.room.discardContent'),
      nzOkText: this.translate.instant('admin.room.stayEditing'),
      nzCancelText: this.translate.instant('admin.room.discardOk'),
      nzClosable: false,
      nzMaskClosable: false,
      nzOnOk: () => {},
      nzOnCancel: () => onLeave(),
    });
  }

  selectRoom(room: Room) {
    if (this.dirty()) {
      this.confirmDiscard(() => this.doSelectRoom(room));
      return;
    }
    this.doSelectRoom(room);
  }

  toggleEditing() {
    if (this.editing() && this.dirty()) {
      this.confirmDiscard(() => {
        this.editing.set(false);
        this.doSelectRoom(this.selectedRoom()!);
      });
      return;
    }
    this.editing.update((v) => !v);
    if (!this.editing()) {
      this.disarm();
      this.lastTap = null;
      this.clearSelection();
      return;
    }
    this.tour.maybeStart();
    // One-time hint: press-and-hold is the only undiscoverable gesture.
    try {
      if (!localStorage.getItem('rooms-hold-hint-shown')) {
        localStorage.setItem('rooms-hold-hint-shown', '1');
        this.toast.createNotification({
          type: 'info',
          title: this.translate.instant('admin.room.noticeTitle'),
          message: this.translate.instant('admin.room.holdHint'),
          position: 'top',
        });
      }
    } catch {
      // storage may be unavailable — hint just reappears
    }
  }

  private doSelectRoom(room: Room) {
    this.selectedRoom.set(room);
    this.clearSelection();
    this.lastTap = null;
    this.deletedGhosts.set(new Set());
    this.dirty.set(false);
    this.editing.set(false);
    this.disarm();
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

  isPending(seat: SeatTemplate): boolean {
    return this.pendingMerge()?.id === seat.id;
  }

  pendingLabel(): string | null {
    const pending = this.pendingMerge();
    if (!pending) return null;
    return this.translate.instant('admin.room.pendingMerge', {
      seat: pending.rowLabel + pending.seatNumber,
    });
  }

  // Cancel a pending double-tap merge (Esc, other actions).
  // NOTE: this must NOT touch lastTap — pointerdown calls disarm() on every
  // tap, and clearing lastTap there would break double-tap detection.
  private disarm() {
    this.pendingMerge.set(null);
  }

  disarmPending() {
    this.disarm();
  }

  private warn(key: string) {
    this.toast.createNotification({
      type: 'warning',
      title: this.translate.instant('admin.room.noticeTitle'),
      message: this.translate.instant(key),
      position: 'top',
    });
  }

  // Click = select/deselect (no modes). Double-tap = merge/split fast path.
  // No timers: a second tap on the same seat within 350ms is a double-tap.
  onSeatTap(seat: SeatTemplate) {
    if (!this.editing()) return;
    if (this.gesture.consumeSuppressedClick()) {
      // Pointer release right after a completed press-and-hold.
      this.lastTap = null;
      return;
    }
    const now = Date.now();
    const isDouble =
      this.lastTap?.id === seat.id && now - this.lastTap.time < 350;
    if (seat.seatType === 'COUPLE') {
      if (this.pendingMerge()) {
        // A couple can never be a merge partner.
        this.warn('admin.room.nonAdjacent');
        this.disarm();
      } else if (isDouble) {
        this.splitPair(seat);
      } else {
        this.togglePair(seat);
      }
      this.lastTap = isDouble ? null : { id: seat.id, time: now };
      return;
    }
    const pending = this.pendingMerge();
    if (pending && pending.id === seat.id) {
      this.disarm();
      this.lastTap = null;
      return;
    }
    // Every tap toggles selection immediately for instant feedback.
    this.toggleSelect(seat);
    if (isDouble) {
      this.lastTap = null;
      if (pending) {
        const [a, b] = [pending, seat].sort((x, y) => x.seatNumber - y.seatNumber);
        if (pending.rowLabel === seat.rowLabel && canMerge([a, b])) {
          this.mergePair(pending, seat);
        } else {
          this.warn('admin.room.nonAdjacent');
        }
        this.disarm();
      } else {
        this.pendingMerge.set(seat);
      }
      return;
    }
    this.lastTap = { id: seat.id, time: now };
  }

  // Additive select used by press-and-hold and row header (never toggles off).
  private addSelection(ids: number[]) {
    if (ids.length === 0) return;
    this.selectedIds.update((set) => {
      const next = new Set(set);
      ids.forEach((id) => next.add(id));
      return next;
    });
    this.lastSelectedId.set(ids[ids.length - 1]);
    this.schedulePill();
    this.tour.advance(0);
  }

  clearSelection() {
    this.selectedIds.set(new Set());
    this.lastSelectedId.set(null);
    this.pillPos.set(null);
    this.typeListOpen.set(false);
  }

  onSeatScroll() {
    if (this.selectedCount() === 0) return;
    this.pillHiddenByScroll.set(true);
    if (this.scrollTimer) clearTimeout(this.scrollTimer);
    this.scrollTimer = setTimeout(() => {
      this.pillHiddenByScroll.set(false);
      this.schedulePill();
    }, 300);
  }

  // Position the floating pill mid-right of the last selected seat: seat
  // tooltips always open above, so the right side never overlaps them.
  // Flip to the left when there is no room on the right (popover-style).
  schedulePill() {
    if (this.selectedCount() === 0) {
      this.pillPos.set(null);
      return;
    }
    requestAnimationFrame(() => {
      const panel = this.layoutPanel?.nativeElement;
      const id = this.lastSelectedId();
      const seatEl = id
        ? panel?.querySelector(`[data-seat-id="${id}"]`)
        : null;
      if (!panel || !seatEl) {
        this.pillPos.set(null);
        return;
      }
      const pr = panel.getBoundingClientRect();
      const sr = seatEl.getBoundingClientRect();
      const top = sr.top - pr.top + panel.scrollTop + sr.height / 2 - 20;
      const rightSide = sr.right - pr.left + panel.scrollLeft + 12;
      if (rightSide + 230 > pr.width) {
        this.pillPos.set({
          top: Math.max(top, 0),
          left: Math.max(sr.left - pr.left + panel.scrollLeft - 230 - 12, 0),
          below: false,
        });
        return;
      }
      this.pillPos.set({ top: Math.max(top, 0), left: rightSide, below: false });
    });
  }

  private findPartner(seat: SeatTemplate): SeatTemplate | undefined {
    return this.layout().find(
      (s) =>
        s.rowLabel === seat.rowLabel &&
        s.seatType === 'COUPLE' &&
        Math.abs(s.seatNumber - seat.seatNumber) === 1,
    );
  }

  // Press-and-hold 500ms to enter selection. Moving >10px cancels (scroll).
  // NOTE: never disarm() here — the first tap of B's double-tap must not
  // wipe the pending A. Pending dies on hold completion and other actions.
  onSeatPointerDown(seat: SeatTemplate, event: PointerEvent) {
    if (!this.editing()) return;
    this.gesture.start(seat, event, (heldSeat) => {
      this.disarm();
      this.lastTap = null;
      if (heldSeat.seatType === 'COUPLE') {
        const partner = this.findPartner(heldSeat);
        this.addSelection(partner ? [heldSeat.id, partner.id] : [heldSeat.id]);
      } else {
        this.addSelection([heldSeat.id]);
      }
    });
  }

  onSeatPointerMove(event: PointerEvent) {
    this.gesture.move(event);
  }

  onSeatPointerUp() {
    this.gesture.end();
  }

  toggleSelect(seat: SeatTemplate) {
    if (!this.editing()) return;
    this.selectedIds.update((set) => {
      const next = new Set(set);
      if (next.has(seat.id)) next.delete(seat.id);
      else next.add(seat.id);
      return next;
    });
    this.lastSelectedId.set(seat.id);
    this.schedulePill();
    this.tour.advance(0);
  }

  selectRow(rowLabel: string) {
    if (!this.editing()) return;
    // Additive only: row header always adds to the current selection.
    this.addSelection(
      this.layout()
        .filter((s) => s.rowLabel === rowLabel)
        .map((s) => s.id),
    );
  }

  applyTypeToSelected(type: SeatTemplateType) {
    if (!this.editing()) return;
    if (type === 'COUPLE') {
      this.makeCoupleFromSelected();
      return;
    }
    const ids = this.selectedIds();
    if (ids.size === 0) return;
    this.disarm();
    this.layout.update((list) =>
      list.map((s) => (ids.has(s.id) ? { ...s, seatType: type } : s)),
    );
    this.dirty.set(true);
  }

  // Creating a couple like any other type: exactly 2 adjacent selected
  // seats become a pair. Anything else is a no-op (the pickers disable
  // the entry unless canMakeCouple()).
  canMakeCouple(): boolean {
    const seats = this.selectedTemplates();
    return seats.length === 2 && canMerge(seats);
  }

  private makeCoupleFromSelected() {
    if (!this.editing()) return;
    const seats = this.selectedTemplates();
    if (seats.length !== 2 || !canMerge(seats)) return;
    this.disarm();
    this.mergePair(seats[0], seats[1]);
  }

  // Fill a specific empty column (layout positions never shift).
  addSeatAt(rowLabel: string, seatNumber: number) {
    if (!this.editing()) return;
    const exists = this.layout().some(
      (s) => s.rowLabel === rowLabel && s.seatNumber === seatNumber,
    );
    if (exists) return;
    this.disarm();
    this.deletedGhosts.update((set) => {
      const next = new Set(set);
      next.delete(`${rowLabel}-${seatNumber}`);
      return next;
    });
    const roomId = this.selectedRoom()?.id ?? 0;
    this.layout.update((list) => [
      ...list,
      {
        id: -Date.now(),
        roomId,
        rowLabel,
        seatNumber,
        seatType: 'NORMAL',
        posX: seatNumber,
        posY: this.grouped().findIndex((g) => g.row === rowLabel),
      },
    ]);
    this.dirty.set(true);
  }

  // Clicking a couple half toggles the whole merged unit.
  togglePair(seat: SeatTemplate) {
    if (!this.editing()) return;
    const partner = this.findPartner(seat);
    const ids = partner ? [seat.id, partner.id] : [seat.id];
    this.selectedIds.update((set) => {
      const next = new Set(set);
      const allIn = ids.every((id) => next.has(id));
      ids.forEach((id) => (allIn ? next.delete(id) : next.add(id)));
      return next;
    });
    this.lastSelectedId.set(seat.id);
    this.schedulePill();
  }

  // Coarse pointers get a bottom sheet (big targets); fine pointers keep
  // the small inline dropdown next to the pill.
  openTypeList() {
    if (this.selectionHasCouple()) return;
    if (this.isCoarsePointer()) {
      const drawer = this.drawerService.create<
        TypeSheetComponent,
        object,
        SeatTemplateType | undefined
      >({
        nzContent: TypeSheetComponent,
        nzContentParams: { coupleAllowed: this.canMakeCouple() },
        nzPlacement: 'bottom',
        nzHeight: 'auto',
        nzClosable: true,
      });
      drawer.afterClose.subscribe((type) => {
        if (type) this.applyTypeToSelected(type);
      });
      return;
    }
    this.typeListOpen.set(!this.typeListOpen());
  }

  selectionHasCouple(): boolean {
    return this.selectedTemplates().some((s) => s.seatType === 'COUPLE');
  }

  selectedTemplates(): SeatTemplate[] {
    const ids = this.selectedIds();
    const byId = new Map(this.layout().map((s) => [s.id, s]));
    return [...ids]
      .map((id) => byId.get(id))
      .filter((s): s is SeatTemplate => !!s)
      .sort((a, b) =>
        a.rowLabel === b.rowLabel
          ? a.seatNumber - b.seatNumber
          : a.rowLabel.localeCompare(b.rowLabel),
      );
  }

  private mergePair(a: SeatTemplate, b: SeatTemplate) {
    const wasSelected =
      this.selectedIds().has(a.id) || this.selectedIds().has(b.id);
    const ids = new Set([a.id, b.id]);
    this.layout.update((list) =>
      list.map((s) => (ids.has(s.id) ? { ...s, seatType: 'COUPLE' } : s)),
    );
    // Spec case #2: an involved ✓ transfers to the new merged pair.
    if (wasSelected) {
      this.selectedIds.set(new Set(ids));
      this.lastSelectedId.set(b.id);
      this.schedulePill();
    } else {
      this.selectedIds.set(new Set());
    }
    this.dirty.set(true);
    this.tour.advance(1);
  }

  private splitPair(seat: SeatTemplate) {
    const partner = this.findPartner(seat);
    if (!partner) return;
    const ids = new Set([seat.id, partner.id]);
    const wasSelected =
      this.selectedIds().has(seat.id) || this.selectedIds().has(partner.id);
    this.layout.update((list) =>
      list.map((s) => (ids.has(s.id) ? { ...s, seatType: 'NORMAL' } : s)),
    );
    // Spec case #3: involved ✓ stays on both children.
    this.selectedIds.set(wasSelected ? new Set(ids) : new Set());
    if (wasSelected) {
      this.lastSelectedId.set(partner.id);
      this.schedulePill();
    }
    this.dirty.set(true);
  }

  deleteSelected() {
    if (!this.editing()) return;
    const seats = this.selectedTemplates();
    if (seats.length === 0) return;
    confirmDelete(
      this.modalService,
      this.translate,
      'admin.room.deleteSeatsTitle',
      () => this.removeSeats(seats),
    );
  }

  // Delete one cell (single seat or a whole merged couple) from the corner ×.
  deleteCellSeats(seats: SeatTemplate[]) {
    if (!this.editing() || seats.length === 0) return;
    this.removeSeats(seats);
  }

  private removeSeats(seats: SeatTemplate[]) {
    this.disarm();
    const ids = new Set(seats.map((s) => s.id));
    this.deletedGhosts.update((set) => {
      const next = new Set(set);
      seats.forEach((s) => next.add(`${s.rowLabel}-${s.seatNumber}`));
      return next;
    });
    this.layout.update((list) => list.filter((s) => !ids.has(s.id)));
    this.selectedIds.update((set) => {
      const next = new Set(set);
      ids.forEach((id) => next.delete(id));
      return next;
    });
    this.dirty.set(true);
  }

  // 3.8 retired: seats are added/removed directly in the grid
  // (empty cell click / corner ×), no −/+ buttons anymore.

  // No per-action confirm in edit mode: nothing persists until Save,
  // and leaving with unsaved changes asks first.
  deleteRow(rowLabel: string) {
    if (!this.editing()) return;
    this.disarm();
    this.layout.update((list) => list.filter((s) => s.rowLabel !== rowLabel));
    this.selectedIds.set(new Set());
    this.dirty.set(true);
  }

  readonly maxRows = 26;

  rowsFull(): boolean {
    return this.grouped().length >= this.maxRows;
  }

  addRow() {
    if (!this.editing() || this.rowsFull()) return;
    this.disarm();
    const rows = this.grouped().map((g) => g.row);
    const nextChar = rows.length
      ? String.fromCharCode(rows[rows.length - 1].charCodeAt(0) + 1)
      : 'A';
    // New rows always fill the fixed 15-column frame.
    const roomId = this.selectedRoom()?.id ?? 0;
    const fresh: SeatTemplate[] = Array.from(
      { length: FIXED_COLUMNS },
      (_, i) => ({
        id: -Date.now() - i,
        roomId,
        rowLabel: nextChar,
        seatNumber: i + 1,
        seatType: 'NORMAL',
        posX: i + 1,
        posY: rows.length,
      }),
    );
    this.layout.update((list) => [...list, ...fresh]);
    this.dirty.set(true);
  }

  // 3.3: duplicate a row right below the source. Rows after it shift
  // one letter down; pairs copy as whole units.
  cloneRow(rowLabel: string) {
    if (!this.editing() || this.rowsFull()) return;
    const labels = this.grouped().map((g) => g.row);
    const idx = labels.indexOf(rowLabel);
    if (idx < 0) return;
    this.disarm();
    const shift = (label: string) =>
      String.fromCharCode(label.charCodeAt(0) + 1);
    const newLabel = shift(rowLabel);
    this.layout.update((list) => {
      const renamed = list.map((s) => {
        const i = labels.indexOf(s.rowLabel);
        return i > idx ? { ...s, rowLabel: shift(s.rowLabel), posY: i + 1 } : s;
      });
      const clones = renamed
        .filter((s) => s.rowLabel === rowLabel)
        .map((s, i) => ({
          ...s,
          id: -Date.now() - i,
          rowLabel: newLabel,
          posY: idx + 1,
        }));
      return [...renamed, ...clones];
    });
    this.selectedIds.set(new Set());
    this.dirty.set(true);
  }

  saveLayout() {
    const room = this.selectedRoom();
    if (!room || !this.dirty()) return;
    this.disarm();
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
        this.tour.advance(2);
        // Spec case #7: selection is temporary UI state — remap it by seat
        // key so it survives the save instead of being cleared.
        const keys = new Set(
          [...this.selectedIds()]
            .map((id) => this.layout().find((s) => s.id === id))
            .filter((s): s is SeatTemplate => !!s)
            .map((s) => `${s.rowLabel}-${s.seatNumber}`),
        );
        this.layout.set(layout);
        const kept = layout
          .filter((s) => keys.has(`${s.rowLabel}-${s.seatNumber}`))
          .map((s) => s.id);
        this.selectedIds.set(new Set(kept));
        if (kept.length > 0) {
          this.lastSelectedId.set(kept[kept.length - 1]);
          this.schedulePill();
        }
        this.deletedGhosts.set(new Set());
        this.dirty.set(false);
        this.saving.set(false);
        // Card totals always reflect the saved layout — never typed by hand.
        const total = layout.length;
        this.rooms.update((list) =>
          list.map((r) => (r.id === room.id ? { ...r, totalSeats: total } : r)),
        );
      },
      error: (err) => {
        this.error.set(errorMessage(err));
        this.saving.set(false);
      },
    });
  }

  seatClass(seatType: string): string {
    return seatTypeClass(seatType);
  }
}
