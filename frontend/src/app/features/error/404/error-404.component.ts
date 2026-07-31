import { Component, inject, ChangeDetectionStrategy } from '@angular/core';
import { Router } from '@angular/router';
import { NzResultModule } from 'ng-zorro-antd/result';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { CommonModule } from '@angular/common';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'app-error-404',
  imports: [
    CommonModule,
    NzResultModule,
    NzButtonModule,
    NzIconModule,
    TranslatePipe,
  ],
  templateUrl: './error-404.component.html',
  styleUrl: './error-404.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Error404Component {
  private router = inject(Router);

  goToHomePage() {
    this.router.navigate(['/']);
  }
}
