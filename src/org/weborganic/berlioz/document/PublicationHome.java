package org.weborganic.berlioz.document;

import java.io.File;
import java.io.FileFilter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.weborganic.berlioz.logging.ZLogger;
import org.weborganic.berlioz.logging.ZLoggerFactory;

/**
 * Handles lists for publications. 
 * 
 * @author Christophe Lauret (Allette Systems)
 * 
 * @version 27 March 2007
 */
public final class PublicationHome {

  /**
   * A logger for the publications.
   */
  private static final ZLogger LOGGER = ZLoggerFactory.getLogger(PublicationHome.class);

  /**
   * The date format for the 'date' prefix used by the files.
   */
  private static final DateFormat DATE_PREFIX_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
 
  /**
   * Filter for the year directory.
   */
  private static final FileFilter YEARS = new FileFilter() {
    public boolean accept(File file) {
      return (file.isDirectory() && file.getName().matches("\\d{4}"));
    }
  };

  /**
   * Filter for the publication file.
   */
  private static final FileFilter PUBLICATION = new FileFilter() {
    public boolean accept(File file) {
      // check that the file is a file
      if (file.isDirectory()) return false;
      String name = file.getName();
      // check that the file is in the correct year
      String year = file.getParentFile().getName();
      if (!name.startsWith(year)) return false;
      // return whether matches the naming convention
      return (name.matches(Document.NAME_PATTERN));
    }
  };

  /**
   * This is where the publications are stored.
   */
  private final File _storage;

  /**
   * Creates a new PublicationHome.
   *
   * @param storage The directory where the publications are stored.
   */
  public PublicationHome(File storage) {
    this._storage = storage;
    LOGGER.debug("Instantiating home for publications with "+storage);
    if (storage == null)
      LOGGER.warn("The publication directory is not setup properly!");
  }

  /**
   * Returns the list of publication currently in the publications' directory.
   * 
   * @return A list of <code>Publication</code> instances 
   */
  public List list() {
    if (this._storage == null) return Collections.EMPTY_LIST;
    ArrayList list = new ArrayList();
    // extract and iterate over the years
    File[] years = this._storage.listFiles(YEARS);
    for (int i = 0; i < years.length; i++) {
      // extract and iterate over the publications
      File[] pubfiles = years[i].listFiles(PUBLICATION);
      for (int j = 0; j < pubfiles.length; j++) {
        list.add(Publication.make(pubfiles[j]));
      }
    }
//    Collections.sort(list);
    return list;
  }

  /**
   * Returns the list of publication currently in the publications' directory.
   * 
   * @param date The effective date (must not be null).
   * @param name The name of the publication (must not be null).
   * 
   * @return A list of <code>Publication</code> instances
   * 
   * @throws IllegalArgumentException Should any parameter be null.
   */
  public List list(Date date, String name) throws IllegalArgumentException {
    if (this._storage == null) return Collections.EMPTY_LIST;
    if (date == null || name == null)
      throw new IllegalArgumentException();
    ArrayList list = new ArrayList();
    String datePrefix = DATE_PREFIX_FORMAT.format(date);
    String yearPrefix = datePrefix.substring(0, 4);
    // extract and iterate over the years
    File directory = new File(this._storage, yearPrefix); 
    File[] pubfiles = directory.listFiles(PUBLICATION);
    for (int j = 0; j < pubfiles.length; j++) {
      if (pubfiles[j].getName().startsWith(datePrefix+"-"+name+"."))
        list.add(Publication.make(pubfiles[j]));
    }
    return list;
  }

}
