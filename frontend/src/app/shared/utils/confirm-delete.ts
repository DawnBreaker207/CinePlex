import { NzModalService } from 'ng-zorro-antd/modal';

export function confirmDelete(
  modalService: NzModalService,
  title: string,
  onOk: () => void,
) {
  modalService.confirm({
    nzTitle: title,
    nzContent: 'Dữ liệu sẽ không thể khôi phục sau khi xóa.',
    nzOkText: 'Xóa',
    nzOkDanger: true,
    nzOnOk: () => onOk(),
    nzCancelText: 'Hủy',
  });
}
