/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at 
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weborganic.berlioz.xml.XMLProperties;

/**
 * Berlioz Web Application release properties.
 *
 * @author Christophe Lauret (Weborganic)
 * @version 11 November 2009
 */
public final class ReleaseProperties {

  /**
   * Displays debug information.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(ReleaseProperties.class);

  /**
   * The ISO date format.
   */
  private static final DateFormat ISO_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

// static variables ---------------------------------------------------------------------------

  /**
   * The global properties.
   */
  private static Properties properties;

// constructor ---------------------------------------------------------------------------------

  /**
   * Prevents the creation of instances.
   */
  private ReleaseProperties() {
    // empty constructor
  }

// properties methods --------------------------------------------------------------------------

  /**
   * Return the requested property.
   *
   * <p>Returns <code>null</code> if the property is not found or defined.
   *
   * <p>If the properties file has not been loaded, this method will invoke the {@link #load()}.
   *
   * @param name The name of the property
   *
   * @return The property value or <code>null</code>.
   * 
   * @throws IllegalStateException If this class has not been setup properly.
   */
  public static String get(String name) throws IllegalStateException {
    if (properties == null) load();
    return properties.getProperty(name);
  }

  /**
   * Returns the requested property or it default value.
   *
   * <p>The given default value is returned only if the property is not found.
   * 
   * <p>If the properties file has not been loaded, this method will invoke the {@link #load()}.
   *
   * @param name  The name of the property.
   * @param def   A default value for the property.
   *
   * @return  the property value or the default value.
   *
   * @throws IllegalStateException If this class has not been setup properly.
   */
  public static String get(String name, String def) throws IllegalStateException {
    if (properties == null) load();
    return properties.getProperty(name, def);
  }

  /**
   * Returns the effective date.
   * 
   * @return The effective date of the current schedule.
   */
  public static Date getEffectiveDate() {
    String date = get("effective-date");
    if (date == null) return getStartOfMonth();
    try {
      return ISO_DATE_FORMAT.parse(date);
    } catch (ParseException ex) {
      LOGGER.warn("Could note parse ISO date: "+date, ex);
      return getStartOfMonth();
    }
  }

  /**
   * Returns the website date.
   * 
   * @return The website date of the current schedule.
   */
  public static Date getWebsiteDate() {
    String date = get("website-date");
    if (date == null) return getStartOfMonth();
    try {
      return ISO_DATE_FORMAT.parse(date);
    } catch (ParseException ex) {
      LOGGER.warn("Could note parse ISO date: "+date, ex);
      return getStartOfMonth();
    }
  }

  /**
   * Returns whether to use the website date or not.
   * 
   * @return boolean of whether to use the website date or not.
   */
  public static String getUseWebsiteDate() {
    String useWebDate = get("use-website-date");
    return useWebDate;
  }

  /**
   * Returns the start of the month as a date.
   * 
   * @return the start of the month as a date.
   */
  public static Date getStartOfMonth() {
    Calendar startOfMonth = Calendar.getInstance();
    startOfMonth.set(Calendar.DAY_OF_MONTH, 1);
    return startOfMonth.getTime();
  }

// setup methods -------------------------------------------------------------------------------

  /**
   * Loads the release properties.
   *
   * @see XMLProperties#load(InputStream)
   * 
   * @return <code>true</code> if the properties were loaded; <code>false</code> otherwise.
   * 
   * @throws IllegalStateException If this class has not been setup properly.
   */
  public static synchronized boolean load() throws IllegalStateException {
    // make sure we have a repository
    File file = new File(GlobalSettings.getRepository(), "data/release.xml");
    try {
      LOGGER.warn("Parsing release information file "+file);
      properties = new XMLProperties();
      FileInputStream is = new FileInputStream(file);
      properties.load(is);
      is.close();
      return true;
    } catch (IOException ex) {
      LOGGER.warn("Could not load release file "+file, ex);
      return false;
    }
  }

}
