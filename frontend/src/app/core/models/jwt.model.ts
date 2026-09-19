import { JwtPayload as JWTDecodePayload } from 'jwt-decode';

export interface Jwt {
  userId: number;
  username: string;
  email: string;
  accessToken: string;
}

export interface JWTPayload extends JWTDecodePayload {
  username?: string;
  email: string;
  roles?: string[];
}

export interface RefreshToken {
  accessToken: string;
}
