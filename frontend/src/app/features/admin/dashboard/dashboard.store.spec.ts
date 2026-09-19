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
    expect(formatDate(start)).toBe('2024-02-01');
    expect(formatDate(end)).toBe('2024-02-29');
  });

  it('quarter 4 crosses year boundary', () => {
    const { start, end } = computeFilterRange('quarter', new Date(2025, 5, 15), 4, 2025);
    expect(formatDate(start)).toBe('2025-10-01');
    expect(formatDate(end)).toBe('2025-12-31');
  });

  it('year covers the full year', () => {
    const { start, end } = computeFilterRange('year', new Date(2025, 5, 15), 1, 2025);
    expect(formatDate(start)).toBe('2025-01-01');
    expect(formatDate(end)).toBe('2025-12-31');
  });
});
