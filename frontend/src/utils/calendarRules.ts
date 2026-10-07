import { Holiday } from '../api';

export interface CalendarDayStatus {
  isNonWorkingDay: boolean;
  holidayType: 'WEEKLY' | 'INSTITUTIONAL' | 'DECLARED' | null;
  reason: string | null;
}

/**
 * Checks if a given date string (YYYY-MM-DD) or Date is a Sunday.
 */
export function isSunday(dateInput: Date | string): boolean {
  const d = typeof dateInput === 'string' ? parseDate(dateInput) : dateInput;
  return d.getDay() === 0;
}

/**
 * Checks if a given date is the second Saturday of its month.
 * Days 1-7 contain the first Saturday.
 * Days 8-14 always contain the second Saturday.
 */
export function isSecondSaturday(dateInput: Date | string): boolean {
  const d = typeof dateInput === 'string' ? parseDate(dateInput) : dateInput;
  return d.getDay() === 6 && d.getDate() >= 8 && d.getDate() <= 14;
}

export function parseDate(dateStr: string): Date {
  const [y, m, d] = dateStr.split('-').map(Number);
  return new Date(y, m - 1, d);
}

/**
 * Determines whether the given date is a non-working day (Sunday, 2nd Saturday, or Declared Holiday).
 */
export function getCalendarDayStatus(dateInput: Date | string, holidays: Holiday[] = []): CalendarDayStatus {
  const d = typeof dateInput === 'string' ? parseDate(dateInput) : dateInput;
  const dateStr = typeof dateInput === 'string' 
    ? dateInput 
    : `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;

  if (d.getDay() === 0) {
    return {
      isNonWorkingDay: true,
      holidayType: 'WEEKLY',
      reason: 'Sunday — Weekly Holiday',
    };
  }

  if (d.getDay() === 6 && d.getDate() >= 8 && d.getDate() <= 14) {
    return {
      isNonWorkingDay: true,
      holidayType: 'INSTITUTIONAL',
      reason: 'Second Saturday — Institutional Holiday',
    };
  }

  const declared = holidays.find((h) => h.date === dateStr);
  if (declared) {
    return {
      isNonWorkingDay: true,
      holidayType: 'DECLARED',
      reason: `Official Holiday Today: ${declared.name}`,
    };
  }

  return {
    isNonWorkingDay: false,
    holidayType: null,
    reason: null,
  };
}
