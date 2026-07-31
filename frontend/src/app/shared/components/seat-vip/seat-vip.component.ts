import { Component, input, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'seat-icon-vip',
  imports: [CommonModule],
  templateUrl: './seat-vip.component.html',
  styleUrl: './seat-vip.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SeatIconVipComponent {
  size = input<number | string>(35);

  className = input<string>('');
}
