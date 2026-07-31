import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { DatePipe } from '@angular/common';
import { NzTagModule } from 'ng-zorro-antd/tag';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { TranslatePipe } from '@ngx-translate/core';
import { Movie } from '@domain/movie/models/movie.model';

@Component({
  selector: 'app-detail-header',
  imports: [DatePipe, NzTagModule, NzButtonModule, NzIconModule, TranslatePipe],
  templateUrl: './detail-header.component.html',
  styleUrls: ['./detail-header.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DetailHeaderComponent {
  readonly movie = input.required<Movie>();
}
