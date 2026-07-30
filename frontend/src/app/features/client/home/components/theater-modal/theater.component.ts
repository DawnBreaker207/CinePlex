import { Component, inject, OnInit, signal } from '@angular/core';
import { NzModalRef } from 'ng-zorro-antd/modal';
import { Theater } from '@domain/theater/models/theater.model';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'app-theater',
  imports: [TranslatePipe],
  templateUrl: './theater.component.html',
})
export class TheaterModalComponent implements OnInit {
  private modelRef = inject(NzModalRef);
  theaters = signal<Theater[]>([]);
  selectedTheaterId = signal<number | null>(null);

  ngOnInit() {
    const { theaters, selectedTheaterId } = this.modelRef.getConfig().nzData;
    this.theaters.set(theaters);
    this.selectedTheaterId.set(selectedTheaterId);
  }

  onSelect(event: Event) {
    const target = event.target as HTMLSelectElement;
    const theaterId = Number(target.value);
    this.selectedTheaterId.set(theaterId);
    this.modelRef.close(theaterId);
  }
}
