export interface Insight {
  type: string;
  severity: 'info' | 'success' | 'warning';
  title: string;
  detail: string;
}