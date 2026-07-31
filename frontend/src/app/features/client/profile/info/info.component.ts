import {
  ChangeDetectorRef,
  Component,
  inject,
  input,
  OnInit,
} from '@angular/core';
import { ChangeDetectionStrategy } from '@angular/core';
import {
  FormControl,
  FormGroup,
  FormsModule,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzGridModule } from 'ng-zorro-antd/grid';
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzTagModule } from 'ng-zorro-antd/tag';
import { NzTooltipModule } from 'ng-zorro-antd/tooltip';
import { NzWaveModule } from 'ng-zorro-antd/core/wave';
import { NzMessageService } from 'ng-zorro-antd/message';
import { UserProfile } from '@domain/user/models/user.model';
import { MovieTicket } from '@features/client/profile/booking-history/booking-history.component';
import { lastValueFrom } from 'rxjs';
import { UserStore } from '@domain/user/data-access/user.store';
import { UploadService } from '@core/services/upload/upload.service';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'app-info',
  imports: [
    FormsModule,
    NzButtonModule,
    NzFormModule,
    NzIconModule,
    NzInputModule,
    NzGridModule,
    NzTagModule,
    NzTooltipModule,
    NzWaveModule,
    ReactiveFormsModule,
    TranslatePipe,
  ],
  templateUrl: './info.component.html',
  styleUrl: './info.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class InfoComponent implements OnInit {
  user = input.required<UserProfile>();

  private fb = inject(NonNullableFormBuilder);
  readonly userStore = inject(UserStore);
  private msg = inject(NzMessageService);
  private cdr = inject(ChangeDetectorRef);
  private uploadService = inject(UploadService);
  private translate = inject(TranslateService);
  selectedFile: File | null = null;
  isSaving: boolean = false;
  selectedTicket: MovieTicket | null = null;
  profileForm!: FormGroup<{
    username: FormControl<string>;
    email: FormControl<string>;
    avatar: FormControl<string>;
    phone: FormControl<string>;
    address: FormControl<string>;
  }>;
  avatarPreview: string = '';

  ngOnInit() {
    this.initializeForm();
    this.avatarPreview = this.user().avatar;
  }

  initializeForm() {
    this.profileForm = this.fb.group({
      username: [this.user().username, [Validators.required]],
      email: [{ value: this.user().email, disabled: true }],
      avatar: [''],
      phone: [
        '0912345678',
        [Validators.required, Validators.pattern(/^[0-9]{10}$/)],
      ],
      address: ['Hà Nội, Việt Nam'],
    });
  }

  onFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;

    if (input.files && input.files.length > 0) {
      const file = input.files[0];

      const isImg = file.type === 'image/png' || file.type === 'image/jpeg';
      if (!isImg) {
        this.msg.error(this.translate.instant('profile.imageTypeError'));
        return;
      }
      const isLt2M = file.size / 1024 / 1024 < 2;
      if (!isLt2M) {
        this.msg.error(this.translate.instant('profile.imageSizeError'));
        return;
      }

      this.getBase64(file, (img: string) => {
        this.avatarPreview = img;
        this.selectedFile = file;

        this.msg.info(this.translate.instant('profile.imageSelected'));
        this.cdr.markForCheck();
      });
    }
  }

  resetAvatar(inputElement: HTMLInputElement) {
    this.avatarPreview = this.user().avatar;
    this.selectedFile = null;
    inputElement.value = '';
    this.msg.info(this.translate.instant('profile.imageReset'));
  }

  async submitProfile() {
    if (this.profileForm.invalid) {
      this.markFormGroup(this.profileForm);
      return;
    }
    this.isSaving = true;
    try {
      let finalAvatarUrl = this.user().avatar;
      if (this.selectedFile) {
        const uploadRes = await lastValueFrom(
          this.uploadService.uploadAssets(this.selectedFile),
        );
        finalAvatarUrl = uploadRes;
      }

      const updatedData: UserProfile = {
        ...this.user(),
        username: this.profileForm.getRawValue().username,
        phone: this.profileForm.getRawValue().phone,
        address: this.profileForm.getRawValue().address,
        avatar: finalAvatarUrl,
      };

      const userId = this.user().userId;

      this.userStore.updateUserProfile({ id: userId, user: updatedData });

      this.msg.success(this.translate.instant('profile.saveSuccess'));
      this.selectedFile = null;
    } catch (error) {
      this.msg.error(this.translate.instant('profile.uploadError'));
    } finally {
      this.isSaving = false;
      this.cdr.markForCheck();
    }
  }

  private markFormGroup(formGroup: FormGroup) {
    Object.values(formGroup.controls).forEach((control) => {
      control.markAsDirty();
      control.updateValueAndValidity({ onlySelf: true });
    });
  }

  private getBase64(img: File, callback: (img: string) => void) {
    const reader = new FileReader();
    reader.addEventListener('load', () => callback(reader.result!.toString()));
    reader.readAsDataURL(img);
  }
}
