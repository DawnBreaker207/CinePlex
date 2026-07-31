import { Component, inject, ChangeDetectionStrategy } from '@angular/core';
import { Router } from '@angular/router';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzResultModule } from 'ng-zorro-antd/result';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { CommonModule } from '@angular/common';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'app-error-500',
  imports: [
    CommonModule,
    NzButtonModule,
    NzResultModule,
    NzIconModule,
    TranslatePipe,
  ],
  templateUrl: './error-500.component.html',
  styleUrl: './error-500.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Error500Component {
  private router = inject(Router);

  goToHomePage() {
    this.router.navigate(['/']);
  }
}
