export interface AuditLog {
  id: number;
  action: string;
  entity: string;
  entityId: string | null;
  actorId: number | null;
  fromState: string | null;
  toState: string | null;
  status: string;
  createdAt: string;
}

export interface AuditFilter {
  action?: string;
  entity?: string;
  userId?: number;
  status?: string;
  from?: string;
  to?: string;
  page?: number;
  size?: number;
}
