import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'isDeleted',
})
export class IsDeletedPipe implements PipeTransform {
  transform(status: boolean) {
    const color = status ? 'red' : 'green';
    // i18n key — render through the translate pipe, never raw.
    const label = status ? 'common.inactive' : 'common.active';
    return { label, color };
  }
}
