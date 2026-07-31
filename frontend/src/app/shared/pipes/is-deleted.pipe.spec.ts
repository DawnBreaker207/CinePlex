import { IsDeletedPipe } from './is-deleted.pipe';

describe('IsDeletedPipe', () => {
  it('create an instance', () => {
    const pipe = new IsDeletedPipe();
    expect(pipe).toBeTruthy();
  });
});
