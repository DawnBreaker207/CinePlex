import { StatusTagsPipe } from './status-tags.pipe';

describe('StatusTagsPipe', () => {
  const pipe = new StatusTagsPipe();

  it('create an instance', () => {
    expect(pipe).toBeTruthy();
  });

  it('maps payment lifecycle states to colors', () => {
    expect(pipe.transform('PAID')).toEqual({ color: 'green', status: 'PAID' });
    expect(pipe.transform('PENDING')).toEqual({ color: 'orange', status: 'PENDING' });
    expect(pipe.transform('REFUND_PENDING')).toEqual({
      color: 'orange',
      status: 'REFUND_PENDING',
    });
    expect(pipe.transform('REFUNDED')).toEqual({ color: 'gray', status: 'REFUNDED' });
    expect(pipe.transform('FAILED')).toEqual({ color: 'red', status: 'FAILED' });
    expect(pipe.transform('CANCELED')).toEqual({ color: 'red', status: 'CANCELED' });
    expect(pipe.transform('EXPIRED')).toEqual({ color: 'default', status: 'EXPIRED' });
  });

  it('maps provider methods to colors', () => {
    expect(pipe.transform('MOMO').color).toBe('purple');
    expect(pipe.transform('VNPAY').color).toBe('blue');
    expect(pipe.transform('ZALOPAY').color).toBe('cyan');
    expect(pipe.transform('CASH').color).toBe('green');
  });

  it('falls back to default for unknown states', () => {
    expect(pipe.transform('SOMETHING_NEW')).toEqual({
      color: 'default',
      status: 'SOMETHING_NEW',
    });
  });
});
