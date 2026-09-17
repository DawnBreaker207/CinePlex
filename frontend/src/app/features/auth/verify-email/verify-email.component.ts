import {
  Component,
  inject,
  OnInit,
  signal,
  ChangeDetectionStrategy,
} from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzResultModule } from 'ng-zorro-antd/result';
import { NzSpinModule } from 'ng-zorro-antd/spin';
import { TranslatePipe } from '@ngx-translate/core';
import { AuthService } from '@core/auth/auth.service';

@Component({
  selector: 'app-verify-email',
  imports: [RouterLink, NzButtonModule, NzResultModule, NzSpinModule, TranslatePipe],
  templateUrl: './verify-email.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class VerifyEmailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private authService = inject(AuthService);

  readonly checking = signal(true);
  readonly verified = signal(false);

  ngOnInit() {
    const token = this.route.snapshot.queryParamMap.get('token') ?? '';
    if (!token) {
      this.checking.set(false);
      return;
    }
    this.authService.verifyEmail(token).subscribe({
      next: () => {
        this.verified.set(true);
        this.checking.set(false);
      },
      error: () => this.checking.set(false),
    });
  }
}
