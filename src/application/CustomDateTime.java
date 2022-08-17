package application;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

/* Custom format: D...HHMM where D = days since day0, HHMM = 24 hour clock (time of day) */
public class CustomDateTime {
	private static final LocalDate day0 = LocalDate.of(2021, 12, 31);

	/* Convert LocalDateTime to CustomDateTime (long) */
	public static long convertDateTime(LocalDateTime localDateTime) {
		long result = 0;
		result += localDateTime.getMinute();
		result += localDateTime.getHour() * 100;
		result += ChronoUnit.DAYS.between(day0, localDateTime.toLocalDate()) * 10000;
		return result;
	}

	/* Convert CustomDateTime (long) to LocalDateTime */
	public static LocalDateTime convertDateTime(long customDateTime) {
		LocalDateTime result = null;
		try {
			String valueAsString = Long.toString(customDateTime);
			int length = valueAsString.length();
			int daysValue = Integer.parseInt(valueAsString.substring(0, length - 4));
			int hourValue = Integer.parseInt(valueAsString.substring(length - 4, length - 2));
			int minuteValue = Integer.parseInt(valueAsString.substring(length - 2, length));
			result = LocalDateTime.of(day0.plusDays(daysValue), LocalTime.of(hourValue, minuteValue));
		} catch (Exception e) {
			result = null;
		}
		return result;
	}

	public static long today() {
		return ChronoUnit.DAYS.between(day0, LocalDate.now()) * 10000;
	}

	public static long[] daysThisWeek() {
		long today = today();
		int dayNumToday = LocalDate.now().getDayOfWeek().getValue();
		long[] daysThisWeek = new long[dayNumToday - 1];
		for (int dayNum = 1; dayNum < dayNumToday; dayNum++) {
			daysThisWeek[dayNum - 1] = today - ((dayNumToday - dayNum) * 10000);
		}
		return daysThisWeek; // the index + 1 of each day is it's DayOfWeek value
	}

	public static long thisWeekStart() {
		return today() - ((LocalDate.now().getDayOfWeek().getValue() - 1) * 10000);
	}

	public static long thisMonthStart() {
		return ChronoUnit.DAYS.between(day0, LocalDate.now().withDayOfMonth(1)) * 10000;
	}
}
