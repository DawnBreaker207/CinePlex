export type SeatTemplateType = 'NORMAL' | 'VIP' | 'COUPLE' | 'WHEELCHAIR';

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
