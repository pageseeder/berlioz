package org.pageseeder.berlioz.util;

import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the {@code java.time}-based {@link ISO8601} against the behavior of the
 * {@code SimpleDateFormat}-based implementation it replaced.
 *
 * <p>The legacy patterns are re-created locally (not by referencing production code) so that
 * these tests keep proving backward compatibility even after {@code SimpleDateFormat} is gone
 * from {@link ISO8601} itself.
 */
class ISO8601Test {

  /** Representative epoch millis spanning past/future, DST/non-DST, and the epoch itself. */
  private static final long[] SAMPLE_EPOCHS = {
      0L,
      1L,
      -1L,
      1049155200000L,   // 2003-04-01T00:00:00Z
      1700000000000L,
      System.currentTimeMillis(),
      -272314585669L,   // before 1970
      2706034849668L,   // far future
  };

  private static SimpleDateFormat legacy(String pattern) {
    return new SimpleDateFormat(pattern);
  }

  // -- YEAR --------------------------------------------------------------

  @Test
  void testYearFormat() {
    String result = ISO8601.YEAR.format(0L);
    assertEquals(4, result.length(), "Year format should produce 4 chars");
    assertTrue(result.matches("\\d{4}"), "Year should be 4 digits");
  }

  @Test
  void testYearFormatMatchesLegacyForAllSamples() {
    SimpleDateFormat legacy = legacy("yyyy");
    for (long epoch : SAMPLE_EPOCHS) {
      assertEquals(legacy.format(new Date(epoch)), ISO8601.YEAR.format(epoch),
          "Mismatch formatting epoch " + epoch);
    }
  }

  @Test
  void testYearParseMatchesLegacy() throws ParseException {
    Date expected = legacy("yyyy").parse("2023");
    Date actual = ISO8601.YEAR.parse("2023");
    assertEquals(expected, actual);
  }

  @Test
  void testYearParseInvalidThrowsParseException() {
    assertThrows(ParseException.class, () -> ISO8601.YEAR.parse("abcd"));
  }

  // -- CALENDAR_DATE -------------------------------------------------------

  @Test
  void testCalendarDateFormat() {
    // 2003-04-01T00:00:00 UTC
    long epoch = 1049155200000L;
    String result = ISO8601.CALENDAR_DATE.format(epoch);
    assertTrue(result.matches("\\d{4}-\\d{2}-\\d{2}"), "Calendar date should be YYYY-MM-DD");
  }

  @Test
  void testCalendarDateFormatMatchesLegacyForAllSamples() {
    SimpleDateFormat legacy = legacy("yyyy-MM-dd");
    for (long epoch : SAMPLE_EPOCHS) {
      assertEquals(legacy.format(new Date(epoch)), ISO8601.CALENDAR_DATE.format(epoch),
          "Mismatch formatting epoch " + epoch);
    }
  }

  @Test
  void testCalendarDateParseMatchesLegacy() throws ParseException {
    Date expected = legacy("yyyy-MM-dd").parse("2023-06-15");
    Date actual = ISO8601.CALENDAR_DATE.parse("2023-06-15");
    assertEquals(expected, actual);
  }

  @Test
  void testCalendarDateRoundTrip() throws ParseException {
    String formatted = ISO8601.CALENDAR_DATE.format(System.currentTimeMillis());
    Date parsed = ISO8601.CALENDAR_DATE.parse(formatted);
    assertNotNull(parsed);
    String reformatted = ISO8601.CALENDAR_DATE.format(parsed.getTime());
    assertEquals(formatted, reformatted);
  }

  @Test
  void testCalendarDateParseInvalidThrowsParseException() {
    assertThrows(ParseException.class, () -> ISO8601.CALENDAR_DATE.parse("not-a-date"));
  }

  // -- TIME ----------------------------------------------------------------

  @Test
  void testTimeFormat() {
    String result = ISO8601.TIME.format(0L);
    assertTrue(result.matches("\\d{2}:\\d{2}:\\d{2}"), "Time should be HH:mm:ss");
  }

  @Test
  void testTimeFormatMatchesLegacyForAllSamples() {
    SimpleDateFormat legacy = legacy("HH:mm:ss");
    for (long epoch : SAMPLE_EPOCHS) {
      assertEquals(legacy.format(new Date(epoch)), ISO8601.TIME.format(epoch),
          "Mismatch formatting epoch " + epoch);
    }
  }

  @Test
  void testTimeParseMatchesLegacy() throws ParseException {
    Date expected = legacy("HH:mm:ss").parse("23:59:59");
    Date actual = ISO8601.TIME.parse("23:59:59");
    assertEquals(expected, actual);
  }

  @Test
  void testTimeParseInvalidThrowsParseException() {
    assertThrows(ParseException.class, () -> ISO8601.TIME.parse("25:99:99"));
  }

  @Test
  void testTimeParseRejectsMissingSeconds() {
    // Seconds must always be present: java.time's own ISO_LOCAL_TIME would accept "10:15", but
    // that would break the fixed-width contract parseAuto relies on.
    assertThrows(ParseException.class, () -> ISO8601.TIME.parse("10:15"));
  }

  @Test
  void testTimeFormatIsFixedWidthEvenWithSubSecondPrecision() {
    // Sub-second precision on the input must never leak into TIME's output.
    assertEquals(ISO8601.TIME.format(1000L), ISO8601.TIME.format(1123L));
  }

  // -- DATETIME --------------------------------------------------------------

  @Test
  void testDateTimeFormat() {
    long now = System.currentTimeMillis();
    String result = ISO8601.DATETIME.format(now);
    // ISO 8601 datetime should contain T and timezone colon separator
    assertTrue(result.contains("T"), "Datetime should contain T separator");
    assertTrue(result.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[+-]\\d{2}:\\d{2}"),
        "Datetime should match ISO 8601 format with colon in timezone, got: " + result);
  }

  @Test
  void testDateTimeFormatMatchesLegacyForAllSamples() {
    SimpleDateFormat legacy = legacy("yyyy-MM-dd'T'HH:mm:ssZ");
    for (long epoch : SAMPLE_EPOCHS) {
      String legacyFormatted = legacy.format(new Date(epoch));
      String legacyWithColon = legacyFormatted.substring(0, legacyFormatted.length() - 2) + ":"
          + legacyFormatted.substring(legacyFormatted.length() - 2);
      assertEquals(legacyWithColon, ISO8601.DATETIME.format(epoch), "Mismatch formatting epoch " + epoch);
    }
  }

  @Test
  void testDateTimeRoundTrip() throws ParseException {
    long now = System.currentTimeMillis() / 1000 * 1000; // truncate millis
    String formatted = ISO8601.DATETIME.format(now);
    Date parsed = ISO8601.DATETIME.parse(formatted);
    assertEquals(now, parsed.getTime());
  }

  @Test
  void testDateTimeRoundTripForAllSamples() throws ParseException {
    for (long epoch : SAMPLE_EPOCHS) {
      long truncated = epoch / 1000 * 1000;
      String formatted = ISO8601.DATETIME.format(truncated);
      Date parsed = ISO8601.DATETIME.parse(formatted);
      assertEquals(truncated, parsed.getTime(), "Round trip mismatch for epoch " + epoch);
    }
  }

  @Test
  void testDateTimeParseInvalidThrowsParseException() {
    assertThrows(ParseException.class, () -> ISO8601.DATETIME.parse("not-a-datetime"));
  }

  @Test
  void testDateTimeParseRejectsMissingSeconds() {
    // ISO_OFFSET_DATE_TIME would accept "2003-04-01T10:00+10:00" (no seconds)
    // DATETIME must not, to keep a fixed-width, backward-compatible contract.
    assertThrows(ParseException.class, () -> ISO8601.DATETIME.parse("2003-04-01T10:00+10:00"));
  }

  @Test
  void testDateTimeFormatNeverIncludesMillisecondsEvenWithSubSecondPrecision() {
    String withoutMillis = ISO8601.DATETIME.format(1049155200000L);
    String withMillis = ISO8601.DATETIME.format(1049155200123L);
    assertEquals(withoutMillis, withMillis, "DATETIME must always truncate to whole seconds");
    assertEquals(withoutMillis.length(), withMillis.length());
  }

  @Test
  void testDateTimeFormatUsesNumericOffsetNeverZuluSuffix() {
    // The offset must always print numerically (e.g. "+00:00" at UTC), never the "Z" shorthand
    // ISO 8601 also allows, to keep the output shape consistent.
    String formatted = ISO8601.DATETIME.format(1049155200000L);
    assertFalse(formatted.endsWith("Z"), "DATETIME should never use the Z shorthand, got: " + formatted);
  }

  @Test
  void testDateTimeParseAcceptsMilliseconds() throws ParseException {
    // Formatting is strict and never shows milliseconds, but parsing is lenient: a value
    // produced by DATETIME_MS (or any other ISO 8601 producer) must still parse correctly.
    Date parsed = ISO8601.DATETIME.parse("2003-04-01T10:00:00.123+10:00");
    assertEquals(1049155200123L, parsed.getTime());
  }

  // -- DATETIME_MS ------------------------------------------------------------

  @Test
  void testDateTimeMsFormatIncludesMillisecondsEvenWhenZero() {
    // Millisecond precision must always be shown, even when exactly zero, to keep fixed width.
    String formatted = ISO8601.DATETIME_MS.format(1049155200000L);
    assertTrue(formatted.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.000[+-]\\d{2}:\\d{2}"),
        "Expected .000 milliseconds, got: " + formatted);
  }

  @Test
  void testDateTimeMsFormatIncludesActualMilliseconds() {
    String formatted = ISO8601.DATETIME_MS.format(1049155200123L);
    assertTrue(formatted.contains(".123"), "Expected .123 milliseconds, got: " + formatted);
  }

  @Test
  void testDateTimeMsRoundTrip() throws ParseException {
    long now = System.currentTimeMillis();
    String formatted = ISO8601.DATETIME_MS.format(now);
    Date parsed = ISO8601.DATETIME_MS.parse(formatted);
    assertEquals(now, parsed.getTime());
  }

  @Test
  void testDateTimeMsParseAcceptsMissingMilliseconds() throws ParseException {
    // Parsing is lenient about the fractional second in both directions: a plain DATETIME
    // value (no milliseconds) must still parse via DATETIME_MS.
    Date parsed = ISO8601.DATETIME_MS.parse("2003-04-01T10:00:00+10:00");
    assertEquals(1049155200000L, parsed.getTime());
  }

  @Test
  void testDateTimeAndDateTimeMsShareParsingBehavior() throws ParseException {
    // Formatting is distinct (strict, fixed width per constant); parsing is unified.
    assertEquals(ISO8601.DATETIME.parse("2003-04-01T10:00:00.123+10:00"),
        ISO8601.DATETIME_MS.parse("2003-04-01T10:00:00.123+10:00"));
    assertEquals(ISO8601.DATETIME.parse("2003-04-01T10:00:00+10:00"),
        ISO8601.DATETIME_MS.parse("2003-04-01T10:00:00+10:00"));
  }

  @Test
  void testDateTimeMsParseInvalidThrowsParseException() {
    assertThrows(ParseException.class, () -> ISO8601.DATETIME_MS.parse("not-a-datetime"));
  }

  // -- WEEK_DATE (proper ISO 8601 week date: week-based year, week of year, ISO day of week) --

  @Test
  void testWeekDateFormatMatchesKnownReferences() {
    // Well-known ISO week date reference points (see Wikipedia: ISO week date).
    assertEquals("2005-W52-7", ISO8601.WEEK_DATE.format(toEpoch(2006, 1, 1)));
    assertEquals("1981-W01-4", ISO8601.WEEK_DATE.format(toEpoch(1981, 1, 1)));
    assertEquals("2004-W53-6", ISO8601.WEEK_DATE.format(toEpoch(2005, 1, 1)));
  }

  @Test
  void testWeekDateFormatMatchesJavadocExample() {
    // Javadoc documents 2003-W14-2 as the week date for 2003-04-01.
    assertEquals("2003-W14-2", ISO8601.WEEK_DATE.format(toEpoch(2003, 4, 1)));
  }

  @Test
  void testWeekDateParseMatchesKnownReferences() throws ParseException {
    assertEquals(new Date(toEpoch(2006, 1, 1)), ISO8601.WEEK_DATE.parse("2005-W52-7"));
    assertEquals(new Date(toEpoch(1981, 1, 1)), ISO8601.WEEK_DATE.parse("1981-W01-4"));
  }

  @Test
  void testWeekDateRoundTripForRandomSamples() throws ParseException {
    Random random = new Random(42);
    for (int i = 0; i < 200; i++) {
      long epoch = Math.floorMod(random.nextLong(), 4102444800000L) - 1000000000000L;
      // truncate to the start of the day since WEEK_DATE has no time component
      long dayStart = epoch / 86400000L * 86400000L;
      String formatted = ISO8601.WEEK_DATE.format(dayStart);
      assertTrue(formatted.matches("\\d{4}-W\\d{2}-\\d"), "Unexpected format: " + formatted);
      Date parsed = ISO8601.WEEK_DATE.parse(formatted);
      assertEquals(ISO8601.WEEK_DATE.format(parsed.getTime()), formatted,
          "Round trip mismatch for epoch " + epoch);
    }
  }

  @Test
  void testWeekDateParseInvalidThrowsParseException() {
    assertThrows(ParseException.class, () -> ISO8601.WEEK_DATE.parse("not-a-week-date"));
  }

  private static long toEpoch(int year, int month, int day) {
    return java.time.LocalDate.of(year, month, day)
        .atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
  }

  // -- Static delegates and parseAuto ----------------------------------------

  @Test
  void testStaticFormatDelegate() {
    long now = System.currentTimeMillis();
    assertEquals(ISO8601.YEAR.format(now), ISO8601.format(now, ISO8601.YEAR));
  }

  @Test
  void testParseAutoYear() throws ParseException {
    Date d = ISO8601.parseAuto("2023");
    assertNotNull(d);
    assertEquals("2023", ISO8601.YEAR.format(d.getTime()));
  }

  @Test
  void testParseAutoCalendarDate() throws ParseException {
    Date d = ISO8601.parseAuto("2023-06-15");
    assertNotNull(d);
    assertEquals("2023-06-15", ISO8601.CALENDAR_DATE.format(d.getTime()));
  }

  @Test
  void testParseAutoWeekDate() throws ParseException {
    // Regression test: parseAuto used to look for 'W' at index 6 instead of 5, so real week
    // dates (e.g. "2003-W14-2", where 'W' is at index 5) fell through to the wrong branch.
    Date d = ISO8601.parseAuto("2003-W14-2");
    assertNotNull(d);
    assertEquals(ISO8601.WEEK_DATE.parse("2003-W14-2"), d);
  }

  @Test
  void testParseAutoTime() throws ParseException {
    Date d = ISO8601.parseAuto("23:59:59");
    assertNotNull(d);
    assertEquals(ISO8601.TIME.parse("23:59:59"), d);
  }

  @Test
  void testParseAutoDateTime() throws ParseException {
    long now = System.currentTimeMillis() / 1000 * 1000;
    String formatted = ISO8601.DATETIME.format(now);
    Date d = ISO8601.parseAuto(formatted);
    assertEquals(now, d.getTime());
  }

  @Test
  void testParseAutoDateTimeMs() throws ParseException {
    long now = System.currentTimeMillis();
    String formatted = ISO8601.DATETIME_MS.format(now);
    Date d = ISO8601.parseAuto(formatted);
    assertEquals(now, d.getTime());
  }

  // -- Thread-safety (java.time formatters are immutable, unlike SimpleDateFormat) -----------

  @Test
  void testConcurrentFormatAndParseAreThreadSafe() throws InterruptedException {
    int threads = 16;
    Thread[] pool = new Thread[threads];
    boolean[] failed = new boolean[threads];
    for (int i = 0; i < threads; i++) {
      final int idx = i;
      pool[i] = new Thread(() -> {
        try {
          for (int j = 0; j < 500; j++) {
            long epoch = System.currentTimeMillis() - j * 1000L;
            String formatted = ISO8601.DATETIME.format(epoch);
            ISO8601.DATETIME.parse(formatted);
          }
        } catch (Exception ex) {
          failed[idx] = true;
        }
      });
    }
    for (Thread t : pool) t.start();
    for (Thread t : pool) t.join();
    for (boolean f : failed) assertFalse(f, "Concurrent format/parse should not fail");
  }
}
