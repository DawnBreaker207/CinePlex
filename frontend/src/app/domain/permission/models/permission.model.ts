import { PermissionKey } from '@core/constants/enum';

export interface UserPermissions {
  userId: number;
  keys: PermissionKey[];
}