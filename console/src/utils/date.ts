//TODO 后续期望 Console 在 shared 包中提供统一的日期时间方法。
import dayjs from "dayjs";
import "dayjs/locale/zh-cn";
import timezone from "dayjs/plugin/timezone";
import utc from "dayjs/plugin/utc";
import relativeTime from "dayjs/plugin/relativeTime";

dayjs.extend(timezone);
dayjs.extend(utc);
dayjs.extend(relativeTime);

dayjs.locale("zh-cn");

export function formatDatetime(date: string | Date | undefined | null, tz?: string): string {
  if (!date) {
    return "";
  }
  return dayjs(date).tz(tz).format("YYYY-MM-DD HH:mm:ss");
}

export function toISOString(date: string | Date | undefined | null): string {
  if (!date) {
    return "";
  }
  return dayjs(date).utc(false).toISOString();
}

export function toDatetimeLocal(date: string | Date | undefined | null, tz?: string): string {
  if (!date) {
    return "";
  }
  // see https://developer.mozilla.org/en-US/docs/Web/HTML/Element/input/datetime-local#the_y10k_problem_often_client-side
  return dayjs(date).tz(tz).format("YYYY-MM-DDTHH:mm");
}

export function toISODayEndOfTime(date: string | Date | undefined | null): string {
  if (!date) {
    return "";
  }
  return dayjs(date).endOf("day").toISOString();
}

/**
 * Get relative time to end date
 *
 * @param date end date
 * @returns relative time to end date
 *
 * @example
 *
 * // now is 2020-12-01
 * RelativeTimeTo("2021-01-01") // in 1 month
 */
export function relativeTimeTo(date: string | Date | undefined | null) {
  if (!date) {
    return;
  }

  return dayjs().to(dayjs(date));
}
