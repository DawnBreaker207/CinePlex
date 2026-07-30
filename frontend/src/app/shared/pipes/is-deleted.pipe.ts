import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'isDeleted',
})
export class IsDeletedPipe implements PipeTransform {
  transform(status: boolean) {
    const color = status ? 'red' : 'green';
    const label = status ? 'Inactive' : 'Active';
    return { label, color };
  }
}
