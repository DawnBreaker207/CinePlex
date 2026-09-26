function getCssVar(name: string, fallback: string): string {
  if (typeof document === 'undefined') return fallback;
  return getComputedStyle(document.documentElement).getPropertyValue(name).trim() || fallback;
}

export const CHART_COLORS = {
  primary: () => getCssVar('--color-primary', '#e4002b'),
  primaryLight: () => getCssVar('--color-primary-light', '#feecf0'),
  success: () => getCssVar('--color-success', '#2d7d46'),
  warning: () => getCssVar('--color-warning', '#d97706'),
  info: () => getCssVar('--color-info', '#2563eb'),
  danger: () => getCssVar('--color-danger', '#dc2626'),
  muted: () => getCssVar('--color-muted', '#6b6b6b'),
  textSecondary: () => getCssVar('--color-text-secondary', '#6b6b6b'),
  border: () => getCssVar('--color-border', '#e9e6e1'),
};

export const ICON_COLORS = {
  edit: () => getCssVar('--color-success', '#2d7d46'),
  delete: () => getCssVar('--color-danger', '#dc2626'),
};
