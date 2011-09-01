/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at 
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * This class provides methods for date formatting and parsing according to ISO 8601.
 * 
 * <p>It is useful for generators as XSLT uses ISO 8601 for dates.
 * 
 * <p>
 * Notation:
 * <ul>
 * <li>YYYY is the year in the Gregorian calendar,</li>
 * <li>ww is the week of the year between 01 (the first week) and 52 or 53 (the last week),</li>
 * <li>MM is the month of the year between 01 (January) and 12 (December),</li>
 * <li>DD is the day of the month between 01 and 31.</li>
 * <li>hh is the number of complete hours that have passed since midnight,</li>
 * <li>mm is the number of complete minutes since the start of the hour,</li>
 * <li>ss is the number of complete seconds since the start of the minute.</li>
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
 * @author Christophe Lauret (Weborganic)
 * @version 3 June 2010
 */
public enum ISO8601 {

  /**
   * The calendar date as defined by ISO 8601, 'YYYY' (Example: 2010).
   */
  YEAR("yyyy"),

  /**
   * The calendar date as defined by ISO 8601, 'YYYY-MM-DD' (Example: 2003-04-01).
   */
  CALENDAR_DATE("yyyy-MM-dd"),

  /**
   * The week date as defined by ISO 8601, 'YYYY-MM-DD' (Example: 2003-W14-2).
   */
  WEEK_DATE("yyyy-'W'WW-d"),

  /**
   * The time of the day as defined by ISO 8601, 'hh:mm:ss' (Example: 23:59:59).
   */
  TIME("HH:mm:ss"),

  /**
   * The Date and time as defined by ISO 8601, 'YYYY-MM-DDThh:mm:ss'.
   */
  DATETIME("yyyy-MM-dd'T'HH:mm:ssZ");

  /**
   * The format string to use with a <code>SimpleDateFormat</code>.
   */
  private final String _format;

  /**
   * Creates a new ISO 8601 format.
   * 
   * @param format The format string to use with a <code>SimpleDateFormat</code>.
   */
  private ISO8601(String format) {
    this._format = format;
  }

  /**
   * Formats the specified date for the specified ISO 8601 format.
   * 
   * @param date The date the format
   * @return the corresponding date as the specified ISO 8601 format.
   */
  public String format(long date) {
    DateFormat iso = new SimpleDateFormat(this._format);
    // the Time Zone component of Java Simple Date Format does not conform to ISO 8601
    // (it does not include the colon to separate hours from minutes)
    if (this._format.charAt(this._format.length() - 1) == 'Z') {
      String formatted = iso.format(date);
      return formatted.substring(0, formatted.length() - 2) + ":"
          + formatted.substring(formatted.length() - 2);
    } else
      return iso.format(date);
  }

  /**
   * Parses the specified date as the specified ISO 8601 format.
   * 
   * @param date The date the format
   * @return the corresponding date as the specified ISO 8601 format.
   * 
   * @throws ParseException Should an error be thrown by the {@link SimpleDateFormat#parse(String)} method.
   */
  public Date parse(String date) throws ParseException {
    DateFormat iso = new SimpleDateFormat(this._format);
    // the Time Zone component of Java Simple Date Format does not conform to ISO 8601
    // (it does not include the colon to separate hours from minutes)
    boolean hasTimeZone = (this._format.charAt(this._format.length() - 1) == 'Z');
    String parsable = date;
    if (hasTimeZone) {
      parsable = date.substring(0, date.length() - 2) + ":" + date.substring(date.length() - 2);
    }
    return iso.parse(parsable);
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
   * @throws ParseException Should an error be thrown by the {@link SimpleDateFormat#parse(String)} method.
   */
  public static Date parseAuto(String date) throws ParseException {
    if (date.length() == 4 && date.matches("\\d{4}"))
      return YEAR.parse(date);
    if (date.indexOf('W') == 6)
      return WEEK_DATE.parse(date);
    if (date.length() == 10)
      return CALENDAR_DATE.parse(date);
    if (date.length() == 8)
      return TIME.parse(date);
    return DATETIME.parse(date);
  }

}
