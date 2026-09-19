import dayjs from 'dayjs';

export const formatDate = (date?: Date | string): string => {
  if (!date) return '';
  return dayjs(date).format('YYYY-MM-DD').toString();
};
export const formatDisplayDate = (date?: Date | string): string => {
  if (!date) return '';
  return dayjs(date).format('DD/MM/YYYY').toString();
};
export const formatTime = (date?: Date | string): string | undefined => {
  if (!date) return undefined;
  return dayjs(date).format('HH:mm:ss').toString();
};
export const timeFormat = (time: Date | string | null | undefined) => {
  if (!time) return '-';
  const today = dayjs().format('YYYY-MM-DD');
  const dateTime = dayjs(`${today} ${time}`, 'YYYY-MM-DD HH:mm:ss');
  if (!dateTime.isValid()) return '-';
  return dateTime.format('HH:mm');
};

export const timeFormatDay = (time: Date | string | null | undefined) => {
  if (!time) return '-';
  const today = dayjs().format('YYYY-MM-DD');
  const dateTime = dayjs(`${today} ${time}`, 'YYYY-MM-DD HH:mm:ss');
  if (!dateTime.isValid()) return '-';
  return dateTime;
};
