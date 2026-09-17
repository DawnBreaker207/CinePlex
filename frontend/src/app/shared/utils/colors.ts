function getCssVar(name: string, fallback: string): string {
  if (typeof document === 'undefined') return fallback;
  return getComputedStyle(document.documentElement).getPropertyValue(name).trim() || fallback;
}

export const CHART_COLORS = {
  primary: () => getCssVar('--color-primary', '#e53e3e'),
  primaryLight: () => getCssVar('--color-primary-light', '#fff5f5'),
  success: () => getCssVar('--color-success', '#2d7d46'),
  warning: () => getCssVar('--color-warning', '#d97706'),
  info: () => getCssVar('--color-info', '#2563eb'),
  danger: () => getCssVar('--color-danger', '#dc2626'),
  muted: () => getCssVar('--color-muted', '#718096'),
  textSecondary: () => getCssVar('--color-text-secondary', '#475569'),
  border: () => getCssVar('--color-border', '#e2e8f0'),
};

export const ICON_COLORS = {
  edit: () => getCssVar('--color-success', '#2d7d46'),
  delete: () => getCssVar('--color-danger', '#dc2626'),
};
