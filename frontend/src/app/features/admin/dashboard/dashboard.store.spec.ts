import { computeFilterRange } from './dashboard.store';
import { formatDate } from '@shared/utils/date.helper';

describe('computeFilterRange', () => {
  it('week starts Monday and ends Sunday', () => {
    const { start, end } = computeFilterRange('week', new Date(2026, 1, 3), 1, 2026);
    expect(formatDate(start)).toBe(formatDate(new Date(2026, 1, 2)));
    expect(formatDate(end)).toBe(formatDate(new Date(2026, 1, 8)));
  });

  it('week starting on Sunday counts back to Monday of previous week', () => {
    const { start, end } = computeFilterRange('week', new Date(2026, 1, 1), 1, 2026);
    expect(formatDate(start)).toBe(formatDate(new Date(2026, 0, 26)));
    expect(formatDate(end)).toBe(formatDate(new Date(2026, 1, 1)));
  });

  it('month covers leap February', () => {
    const { start, end } = computeFilterRange('month', new Date(2024, 1, 15), 1, 2024);
    expect(formatDate(start)).toBe('01/02/2024');
    expect(formatDate(end)).toBe('29/02/2024');
  });

  it('quarter 4 crosses year boundary', () => {
    const { start, end } = computeFilterRange('quarter', new Date(2025, 5, 15), 4, 2025);
    expect(formatDate(start)).toBe('01/10/2025');
    expect(formatDate(end)).toBe('31/12/2025');
  });

  it('year covers the full year', () => {
    const { start, end } = computeFilterRange('year', new Date(2025, 5, 15), 1, 2025);
    expect(formatDate(start)).toBe('01/01/2025');
    expect(formatDate(end)).toBe('31/12/2025');
  });
});
