import { DateModel, IsDeleted } from '@core/models/common.model';

export interface User extends DateModel, IsDeleted {
  userId: number;
  username: string;
  avatar: string;
  email: string;
  phone: string;
  address: string;
  role: string;
  isActive: boolean;
}

export interface UserProfile {
  userId: number;
  username: string;
  avatar: string;
  email: string;
  phone: string;
  address: string;
  role: string;
  isActive: boolean;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
}

export interface LoginRequest {
  identifier: string;
  password: string;
}
