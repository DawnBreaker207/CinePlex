import { Component, input, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-seat-icon',
  imports: [CommonModule],
  templateUrl: './seat.component.html',
  styleUrl: './seat.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SeatIconComponent {
  size = input<number | string>(24);

  className = input<string>('');

  outline = input(false);

  // 'couple' renders one long bench in a 2:1 frame, 'vip' renders the
  // chair with a crown — type is recognizable by shape in every status.
  variant = input<'single' | 'couple' | 'vip'>('single');
}
