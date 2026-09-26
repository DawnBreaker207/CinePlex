// Last-resort fallback lives here (single place) so no store ever renders
// a blank error. Prefer server/translated messages — this only fires when
// the backend sent nothing usable (e.g. offline).
export function errorMessage(err: unknown): string {
  const e = err as { error?: { message?: string }; message?: string } | null;
  return e?.error?.message || e?.message || 'Đã có lỗi xảy ra, vui lòng thử lại.';
}
