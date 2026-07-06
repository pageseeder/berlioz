/*
 * Copyright 2015 Allette Systems (Australia)
 * http://www.allette.com.au
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.pageseeder.berlioz.util;

import java.text.ParseException;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.Year;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Date;

/**
 * This class provides methods for date formatting and parsing according to ISO 8601.
 *
 * <p>It is useful for generators as XSLT uses ISO 8601 for dates.
 *
 * <p>All formatters are pre-built, immutable {@link DateTimeFormatter} instances, so every
 * method on this class is safe to call concurrently from multiple threads.
 *
 * <p>
 * Notation:
 * <ul>
 * <li>YYYY is the year in the Gregorian calendar,</li>
 * <li>ww is the ISO week of the week-based year, between 01 and 52 or 53,</li>
 * <li>MM is the month of the year between 01 (January) and 12 (December),</li>
 * <li>DD is the day of the month between 01 and 31.</li>
 * <li>hh is the number of complete hours that have passed since midnight,</li>
 * <li>mm is the number of complete minutes since the start of the hour,</li>
 * <li>ss is the number of complete seconds since the start of the minute,</li>
 * <li>fff is the number of milliseconds since the start of the second (only {@link #DATETIME_MS}).</li>
 * </ul>
 *
 * <p>
 * The capital letter T is used to separate the date and time components.
 *
 * @see <a href="http://en.wikipedia.org/wiki/ISO_8601">Wikipedia: ISO 8601</a>
 * @see <a href="http://www.w3.org/TR/NOTE-datetime">W3C Note: Date and Time Formats</a>
 * @see <a href="http://www.iso.org/iso/date_and_time_format">ISO: Numeric representation of Dates
 *      and Time</a>
 *
 * @author Christophe Lauret
 *
 * @version 0.9.4
 * @since 0.6
 */
public enum ISO8601 {

  /**
   * The calendar date as defined by ISO 8601, 'YYYY' (Example: 2010).
   */
  YEAR(yearFormatter()),

  /**
   * The calendar date as defined by ISO 8601, 'YYYY-MM-DD' (Example: 2003-04-01).
   */
  CALENDAR_DATE(DateTimeFormatter.ISO_LOCAL_DATE),

  /**
   * The week date as defined by ISO 8601, 'YYYY-Www-D' (Example: 2003-W14-2), where 'ww' is the
   * ISO week of the week-based year and 'D' is the ISO day of the week (1=Monday, 7=Sunday).
   */
  WEEK_DATE(DateTimeFormatter.ISO_WEEK_DATE),

  /**
   * The time of the day as defined by ISO 8601, 'hh:mm:ss' (Example: 23:59:59). Seconds are
   * always included, even when zero.
   */
  TIME(timeFormatter()),

  /**
   * The date and time as defined by ISO 8601, 'YYYY-MM-DDThh:mm:ss+hh:mm' (Example:
   * 2003-04-01T10:00:00+10:00). When formatting, seconds are always included, even when zero,
   * and milliseconds are never included (see {@link #DATETIME_MS} for millisecond precision).
   * When parsing, an optional fractional second is accepted (and discarded) for leniency.
   */
  DATETIME(dateTimeFormatter(false), dateTimeParser()),

  /**
   * Same as {@link #DATETIME} but always includes exactly 3 digits of millisecond precision
   * when formatting, even when zero (Example: 2003-04-01T10:00:00.000+10:00). Parsing is
   * equally lenient: the fractional second may be omitted.
   */
  DATETIME_MS(dateTimeFormatter(true), dateTimeParser());

  /**
   * The formatter used to format this ISO 8601 representation.
   */
  private final DateTimeFormatter formatter;

  /**
   * The formatter used to parse this ISO 8601 representation; same as {@link #formatter} except
   * for {@link #DATETIME} and {@link #DATETIME_MS}, which share a single lenient parser that
   * accepts an optional fractional second regardless of whether milliseconds are formatted.
   */
  private final DateTimeFormatter parser;

  /**
   * Creates a new ISO 8601 format whose formatter is also used for parsing.
   *
   * @param formatter The formatter to use.
   */
  ISO8601(DateTimeFormatter formatter) {
    this(formatter, formatter);
  }

  /**
   * Creates a new ISO 8601 format with distinct formatter and parser.
   *
   * @param formatter The formatter to use when formatting.
   * @param parser    The formatter to use when parsing.
   */
  ISO8601(DateTimeFormatter formatter, DateTimeFormatter parser) {
    this.formatter = formatter;
    this.parser = parser;
  }

  /**
   * Formats the specified date for the specified ISO 8601 format.
   *
   * @param date The date the format
   * @return the corresponding date as the specified ISO 8601 format.
   */
  public String format(long date) {
    ZonedDateTime zdt = Instant.ofEpochMilli(date).atZone(ZoneId.systemDefault());
    switch (this) {
      case YEAR:
      case CALENDAR_DATE:
      case WEEK_DATE:
        return this.formatter.format(zdt.toLocalDate());
      case TIME:
        return this.formatter.format(zdt.toLocalTime());
      case DATETIME:
      case DATETIME_MS:
      default:
        return this.formatter.format(zdt);
    }
  }

  /**
   * Parses the specified date as the specified ISO 8601 format.
   *
   * @param date The date the format
   * @return the corresponding date as the specified ISO 8601 format.
   *
   * @throws ParseException Should the date not match the expected format.
   */
  public Date parse(String date) throws ParseException {
    try {
      switch (this) {
        case YEAR:
          return atStartOfDay(LocalDate.of(Year.parse(date, this.parser).getValue(), Month.JANUARY, 1));
        case CALENDAR_DATE:
        case WEEK_DATE:
          return atStartOfDay(LocalDate.parse(date, this.parser));
        case TIME:
          return atStartOfDay1970(LocalTime.parse(date, this.parser));
        case DATETIME:
        case DATETIME_MS:
        default:
          return Date.from(OffsetDateTime.parse(date, this.parser).toInstant());
      }
    } catch (DateTimeException ex) {
      ParseException pe = new ParseException("Unparseable date: \"" + date + "\"", 0);
      pe.initCause(ex);
      throw pe;
    }
  }

  /**
   * Returns the specified date as ISO 8601 format.
   *
   * @param date   the specified date.
   * @param format the ISO 8601 format to use.
   * @return the date formatted using ISO 8601.
   */
  public static String format(long date, ISO8601 format) {
    return format.format(date);
  }

  /**
   * Returns the specified date as ISO 8601 format.
   *
   * @param date the specified date.
   * @return the date formatted using ISO 8601.
   *
   * @throws ParseException Should an error be thrown while parsing the date.
   */
  public static Date parseAuto(String date) throws ParseException {
    if (date.length() == 4 && date.matches("\\d{4}"))
      return YEAR.parse(date);
    if (date.indexOf('W') == 5)
      return WEEK_DATE.parse(date);
    if (date.length() == 10)
      return CALENDAR_DATE.parse(date);
    if (date.length() == 8)
      return TIME.parse(date);
    // DATETIME's parser is lenient about the fractional second, so it also covers DATETIME_MS.
    return DATETIME.parse(date);
  }

  /**
   * @return a strict, fixed-width 4-digit year formatter (no {@code DateTimeFormatter} constant
   *         exists for this in the JDK).
   */
  private static DateTimeFormatter yearFormatter() {
    return new DateTimeFormatterBuilder().appendValue(ChronoField.YEAR, 4).toFormatter();
  }

  /**
   * @return a strict 'hh:mm:ss' formatter where every field is mandatory (unlike
   *         {@link DateTimeFormatter#ISO_LOCAL_TIME}, which allows the seconds to be omitted on
   *         parsing) so the output is always the same width.
   */
  private static DateTimeFormatter timeFormatter() {
    return new DateTimeFormatterBuilder()
        .appendValue(ChronoField.HOUR_OF_DAY, 2)
        .appendLiteral(':')
        .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
        .appendLiteral(':')
        .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
        .toFormatter();
  }

  /**
   * @param includeMillis whether to append a mandatory, always 3-digit millisecond component.
   * @return a strict 'yyyy-MM-ddThh:mm:ss[.fff]+hh:mm' formatter where every field is mandatory
   *         and the offset always uses a colon and is never abbreviated to 'Z' (unlike
   *         {@link DateTimeFormatter#ISO_OFFSET_DATE_TIME}, which allows seconds to be omitted
   *         and only prints a fractional second when one is present), so the output is always
   *         the same width.
   */
  private static DateTimeFormatter dateTimeFormatter(boolean includeMillis) {
    DateTimeFormatterBuilder builder = new DateTimeFormatterBuilder()
        .append(DateTimeFormatter.ISO_LOCAL_DATE)
        .appendLiteral('T')
        .appendValue(ChronoField.HOUR_OF_DAY, 2)
        .appendLiteral(':')
        .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
        .appendLiteral(':')
        .appendValue(ChronoField.SECOND_OF_MINUTE, 2);
    if (includeMillis) {
      builder.appendLiteral('.').appendValue(ChronoField.MILLI_OF_SECOND, 3);
    }
    return builder.appendOffset("+HH:MM", "+00:00").toFormatter();
  }

  /**
   * @return a lenient 'yyyy-MM-ddThh:mm:ss[.f...]+hh:mm' parser, shared by {@link #DATETIME} and
   *         {@link #DATETIME_MS}, that accepts an optional fractional second of any length
   *         (discarded beyond millisecond precision by {@link Instant#toEpochMilli()} when
   *         converted to a {@link Date}) regardless of which constant's format produced it.
   */
  private static DateTimeFormatter dateTimeParser() {
    return new DateTimeFormatterBuilder()
        .append(DateTimeFormatter.ISO_LOCAL_DATE)
        .appendLiteral('T')
        .appendValue(ChronoField.HOUR_OF_DAY, 2)
        .appendLiteral(':')
        .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
        .appendLiteral(':')
        .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
        .optionalStart()
        .appendFraction(ChronoField.NANO_OF_SECOND, 1, 9, true)
        .optionalEnd()
        .appendOffset("+HH:MM", "+00:00")
        .toFormatter();
  }

  private static Date atStartOfDay(LocalDate date) {
    return Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
  }

  private static Date atStartOfDay1970(LocalTime time) {
    LocalDateTime dateTime = LocalDate.of(1970, Month.JANUARY, 1).atTime(time);
    return Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
  }

}
