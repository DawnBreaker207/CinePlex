import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'formattedCountdown',
})
export class FormattedCountdownPipe implements PipeTransform {
  transform(ttl: number | null | undefined): string {
    if (!ttl) return '0:00';
    const minutes = Math.floor(ttl / 60);
    const seconds = ttl % 60;
    return `${minutes}:${seconds.toString().padStart(2, '0')}`;
  }
}
