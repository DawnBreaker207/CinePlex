import { TranslateService } from '@ngx-translate/core';
import { NzModalService } from 'ng-zorro-antd/modal';

export function confirmDelete(
  modalService: NzModalService,
  translate: TranslateService,
  titleKey: string,
  onOk: () => void,
) {
  modalService.confirm({
    nzTitle: translate.instant(titleKey),
    nzContent: translate.instant('common.deleteConfirm'),
    nzOkText: translate.instant('common.delete'),
    nzOkDanger: true,
    nzOnOk: () => onOk(),
    nzCancelText: translate.instant('common.cancel'),
  });
}
