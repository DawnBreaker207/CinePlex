// Fixed-frame seat layout: every row has exactly FIXED_COLUMNS columns.
// A cell is either filled (a seat) or empty (no seat — layout never shifts).
// A couple unit = TWO adjacent filled cells both typed COUPLE, rendered as
// one double-wide button labeled "5+6". Booking selects both instances.
export const FIXED_COLUMNS = 15;

export const COUPLE_SPAN = 2;

export const spanOf = (seatType: string): number =>
  seatType === 'COUPLE' ? COUPLE_SPAN : 1;

// Client booking view: drop leading/trailing empty cells so the visible
// seats center under the screen. Interior gaps (aisles) are preserved.
export function trimOuterEmptyCells<T>(cells: FixedCell<T>[]): FixedCell<T>[] {
  const first = cells.findIndex((c) => c.seats.length > 0);
  if (first < 0) return cells;
  let last = cells.length - 1;
  while (last > first && cells[last].seats.length === 0) last--;
  return cells.slice(first, last + 1);
}

// Background/border/text classes per seat type (single source of truth for
// the admin editor grid and legend). COUPLE purple is the one quarantined
// non-token hue (DESIGN.md); everything else rides tokens with dark variants.
export function seatTypeClass(seatType: string): string {
  switch (seatType) {
    case 'VIP':
      // Fixed ink on gold (both modes): white-on-gold fails contrast.
      return 'bg-[var(--color-gold)] text-[#1a1a1a] border-[var(--color-gold)]';
    case 'COUPLE':
      return 'bg-purple-500 text-white border-purple-500';
    default:
      return 'bg-slate-200 text-slate-600 border-slate-300 dark:bg-gray-700 dark:text-gray-200 dark:border-gray-600';
  }
}

export interface FixedCell<T> {
  /** Column number (1-based). */
  number: number;
  /** Seats in this cell: [] = empty, [s] = single, [a, b] = merged couple. */
  seats: T[];
  span: number;
  /** "5" for singles, "5+6" for merged couples. */
  label: string;
}

// Build the fixed 15-column row. Consecutive COUPLE seats pair up
// left-to-right into merged units; a leftover renders single-width.
export function buildFixedRow<T extends { seatNumber: number; seatType: string }>(
  seats: T[],
  columns = FIXED_COLUMNS,
): FixedCell<T>[] {
  const byNumber = new Map(seats.map((s) => [s.seatNumber, s]));
  // Never drop seats: legacy rows wider than the frame still render fully.
  const total = Math.max(
    columns,
    ...seats.map((s) => s.seatNumber),
    0,
  );
  const cells: FixedCell<T>[] = [];
  let n = 1;
  while (n <= total) {
    const seat = byNumber.get(n) ?? null;
    const next = byNumber.get(n + 1) ?? null;
    if (
      seat &&
      seat.seatType === 'COUPLE' &&
      next &&
      next.seatType === 'COUPLE'
    ) {
      cells.push({ number: n, seats: [seat, next], span: COUPLE_SPAN, label: `${n}+${n + 1}` });
      n += 2;
    } else if (seat) {
      cells.push({ number: n, seats: [seat], span: 1, label: `${n}` });
      n += 1;
    } else {
      cells.push({ number: n, seats: [], span: 1, label: `${n}` });
      n += 1;
    }
  }
  return cells;
}

// Two selected seats can merge into a couple unit when they are adjacent,
// filled, in the same row, and neither is already COUPLE.
export function canMerge<T extends { seatNumber: number; seatType: string }>(
  seats: T[],
): boolean {
  if (seats.length !== 2) return false;
  const [a, b] = [...seats].sort((x, y) => x.seatNumber - y.seatNumber);
  return (
    b.seatNumber === a.seatNumber + 1 &&
    a.seatType !== 'COUPLE' &&
    b.seatType !== 'COUPLE'
  );
}

// A merged couple (exactly the two halves) can split back to NORMAL.
export function canSplit<T extends { seatNumber: number; seatType: string }>(
  seats: T[],
): boolean {
  if (seats.length !== 2) return false;
  const [a, b] = [...seats].sort((x, y) => x.seatNumber - y.seatNumber);
  return (
    b.seatNumber === a.seatNumber + 1 &&
    a.seatType === 'COUPLE' &&
    b.seatType === 'COUPLE'
  );
}
