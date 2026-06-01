package org.pageseeder.berlioz.util;

import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class ISO8601Test {

  @Test
  void testYearFormat() {
    String result = ISO8601.YEAR.format(0L);
    assertEquals(4, result.length(), "Year format should produce 4 chars");
    assertTrue(result.matches("\\d{4}"), "Year should be 4 digits");
  }

  @Test
  void testCalendarDateFormat() {
    // 2003-04-01T00:00:00 UTC
    long epoch = 1049155200000L;
    String result = ISO8601.CALENDAR_DATE.format(epoch);
    assertTrue(result.matches("\\d{4}-\\d{2}-\\d{2}"), "Calendar date should be YYYY-MM-DD");
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
  void testDateTimeFormat() {
    long now = System.currentTimeMillis();
    String result = ISO8601.DATETIME.format(now);
    // ISO 8601 datetime should contain T and timezone colon separator
    assertTrue(result.contains("T"), "Datetime should contain T separator");
    assertTrue(result.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[+-]\\d{2}:\\d{2}"),
        "Datetime should match ISO 8601 format with colon in timezone, got: " + result);
  }

  @Test
  void testDateTimeRoundTrip() throws ParseException {
    long now = System.currentTimeMillis() / 1000 * 1000; // truncate millis
    String formatted = ISO8601.DATETIME.format(now);
    Date parsed = ISO8601.DATETIME.parse(formatted);
    assertEquals(now, parsed.getTime());
  }

  @Test
  void testTimeFormat() {
    String result = ISO8601.TIME.format(0L);
    assertTrue(result.matches("\\d{2}:\\d{2}:\\d{2}"), "Time should be HH:mm:ss");
  }

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
}
