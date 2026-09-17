export interface Room {
  id: number;
  theaterId: number;
  name: string;
  roomType: string;
  totalSeats: number;
  seatCount: number;
}

export interface RoomRequest {
  name: string;
  roomType: string;
  totalSeats: number;
}
