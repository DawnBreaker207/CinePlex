import { VoucherStatusPipe } from './voucher-status.pipe';

describe('VoucherStatusPipe', () => {
  const pipe = new VoucherStatusPipe();

  it('create an instance', () => {
    expect(pipe).toBeTruthy();
  });

  it('maps voucher lifecycle states to label keys and tag colors', () => {
    expect(pipe.transform('ACTIVE')).toEqual({
      label: 'voucher.status.active',
      color: 'success',
    });
    expect(pipe.transform('PAUSED')).toEqual({
      label: 'voucher.status.paused',
      color: 'warning',
    });
    expect(pipe.transform('SCHEDULED')).toEqual({
      label: 'voucher.status.scheduled',
      color: 'warning',
    });
    expect(pipe.transform('PENDING_APPROVAL')).toEqual({
      label: 'voucher.status.pending',
      color: 'warning',
    });
    expect(pipe.transform('EXPIRED')).toEqual({
      label: 'voucher.status.expired',
      color: 'error',
    });
    expect(pipe.transform('EXHAUSTED')).toEqual({
      label: 'voucher.status.exhausted',
      color: 'error',
    });
  });

  it('falls back to default for unknown states', () => {
    expect(pipe.transform('SOMETHING_NEW')).toEqual({
      label: 'common.unknown',
      color: 'default',
    });
  });
});
