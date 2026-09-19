export type SeatTemplateType = 'NORMAL' | 'VIP' | 'COUPLE';

export const SEAT_TYPES: SeatTemplateType[] = [
  'NORMAL',
  'VIP',
  'COUPLE',
];

// Fixed side aisles: walkways run along both walls, seats fill the middle.
// Seat numbers are contiguous; the editor/client render one aisle slot
// on each side of every row.
export const SIDE_AISLE_SLOTS = 1;

// Max bookable units per row (Beta-style rooms cap at 15).
export const MAX_SEATS_PER_ROW = 15;

export interface SeatTemplate {
  id: number;
  roomId: number;
  rowLabel: string;
  seatNumber: number;
  seatType: string;
  posX: number;
  posY: number;
}

export interface SeatTemplateRequest {
  rowLabel: string;
  seatNumber: number;
  seatType: string;
  posX: number;
  posY: number;
}
