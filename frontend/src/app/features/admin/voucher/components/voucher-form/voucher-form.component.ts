import {
  Component,
  inject,
  OnInit,
  ChangeDetectionStrategy,
} from '@angular/core';
import {
  FormControl,
  FormGroup,
  FormsModule,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { NZ_MODAL_DATA, NzModalModule, NzModalRef } from 'ng-zorro-antd/modal';
import { VoucherStore } from '@domain/voucher/data-access/voucher.store';
import { Voucher } from '@domain/voucher/model/voucher.model';
import {
  DiscountType,
  VoucherCategory,
  VoucherRequest,
  VoucherStatus,
} from '@domain/voucher/model/voucher.model';
import { Mode } from '@core/constants/enum';
import { NzFormModule } from 'ng-zorro-antd/form';
import { TranslatePipe } from '@ngx-translate/core';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzDatePickerModule } from 'ng-zorro-antd/date-picker';
import { NzSelectModule } from 'ng-zorro-antd/select';
import { NzInputNumberModule } from 'ng-zorro-antd/input-number';
import { NzButtonModule } from 'ng-zorro-antd/button';

@Component({
  selector: 'app-voucher-form',
  imports: [
    FormsModule,
    ReactiveFormsModule,
    NzFormModule,
    TranslatePipe,
    NzInputModule,
    NzDatePickerModule,
    NzSelectModule,
    NzInputNumberModule,
    NzButtonModule,
    NzModalModule,
  ],
  templateUrl: './voucher-form.component.html',
  styleUrl: './voucher-form.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FormVoucherComponent implements OnInit {
  private fb = inject(NonNullableFormBuilder);
  private modalRef = inject(NzModalRef);
  readonly voucherStore = inject(VoucherStore);
  readonly nzModalData = inject(NZ_MODAL_DATA, { optional: true });

  form!: FormGroup<{
    name: FormControl<string>;
    code: FormControl<string>;
    rangeDate: FormControl<Date[]>;
    category: FormControl<VoucherCategory>;
    quantityTotal: FormControl<number>;
    discountType: FormControl<DiscountType>;
    discountValue: FormControl<number>;
    maxDiscountAmount: FormControl<number>;
    minOrderValue: FormControl<number>;
    conditions: FormControl<string>;
    groupRef: FormControl<string>;
    status: FormControl<VoucherStatus>;
  }>;
  mode: Mode = 'view';
  voucherId?: number;

  readonly VoucherCategory = VoucherCategory;
  readonly DiscountType = DiscountType;
  readonly statuses: VoucherStatus[] = [
    'SCHEDULED',
    'ACTIVE',
    'PAUSED',
  ];

  parserPercent = (value: number) => `${value} %`;
  parserMoney = (value: number) =>
    `${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, '.');
  parserNumber = (value: string) => {
    const result = value.replace(/\D/g, '');
    return result ? Number(result) : 0;
  };
  ngOnInit() {
    this.initForm();

    const { mode, id } = this.nzModalData || this.modalRef.getConfig().nzData;
    this.mode = mode;
    this.voucherId = id;

    if (this.mode !== 'add' && this.voucherId) {
      this.loadData(this.voucherId);
    }
  }

  initForm() {
    this.form = this.fb.group({
      name: ['', [Validators.required]],
      code: ['', [Validators.required, Validators.pattern(/^[A-Z0-9_-]+$/)]],
      rangeDate: [[] as Date[], [Validators.required]],
      category: [VoucherCategory.CAMPAIGN, [Validators.required]],
      quantityTotal: [100, [Validators.required, Validators.min(1)]],
      discountType: [DiscountType.FIXED, [Validators.required]],
      discountValue: [0, [Validators.required, Validators.min(0)]],
      maxDiscountAmount: [0],
      minOrderValue: [0],
      conditions: [''],
      groupRef: [''],
      status: ['ACTIVE' as VoucherStatus, [Validators.required]],
    });
  }

  private loadData(id: number) {
    const voucher = this.voucherStore.vouchers().find((v) => v.id === id);

    if (voucher) {
      this.patchFormValue(voucher);
      if (this.mode === 'view') {
        this.form.disable();
      }
    }
  }

  private patchFormValue(voucher: Voucher) {
    this.form.patchValue({
      name: voucher.name,
      code: voucher.code,
      rangeDate: [new Date(voucher.startAt), new Date(voucher.endAt)],
      category: voucher.category,
      quantityTotal: voucher.quantityTotal,
      discountType: voucher.discountType,
      discountValue: voucher.discountValue,
      maxDiscountAmount: voucher.maxDiscountAmount,
      minOrderValue: voucher.minOrderValue,
      conditions: voucher.conditions,
      groupRef: voucher.groupRef,
      status: voucher.status ?? 'ACTIVE',
    });
  }

  submit() {
    if (this.form.invalid) {
      Object.values(this.form.controls).forEach((control) => {
        if (control.invalid) {
          control.markAsDirty();
          control.updateValueAndValidity({ onlySelf: true });
        }
      });
      return;
    }
    const formValue = this.form.getRawValue();
    const request: VoucherRequest = {
      name: formValue.name,
      code: formValue.code,
      startAt: formValue.rangeDate[0],
      endAt: formValue.rangeDate[1],
      quantityTotal: formValue.quantityTotal,
      category: formValue.category,
      groupRef: formValue.groupRef,
      conditions: formValue.conditions,
      discountType: formValue.discountType,
      discountValue: formValue.discountValue,
      maxDiscountAmount: formValue.maxDiscountAmount || 0,
      minOrderValue: formValue.minOrderValue || 0,
      status: formValue.status,
      maxPerUser: 1,
    };
    if (this.mode === 'edit' && this.voucherId) {
      this.voucherStore.updateVoucher({
        id: this.voucherId,
        voucher: request,
      });
    } else if (this.mode === 'add') {
      this.voucherStore.createVoucher(request);
    }
    this.modalRef.close(true);
  }

  close() {
    this.modalRef.close(false);
  }
}
