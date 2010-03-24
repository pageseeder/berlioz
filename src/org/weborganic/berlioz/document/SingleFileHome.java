package org.weborganic.berlioz.document;

import java.io.File;

import org.weborganic.berlioz.logging.ZLogger;
import org.weborganic.berlioz.logging.ZLoggerFactory;

/**
 * Handles Single File. 
 * 
 * @author William Liem (Allette Systems)
 * 
 * @version 28 January 2009
 */
public final class SingleFileHome {

  /**
   * A logger for the Single Binary File.
   */
  private static final ZLogger LOGGER = ZLoggerFactory.getLogger(SingleFileHome.class);

  /**
   * This is where the binary files are stored.
   */
  private final File _storage;

  /**
   * Creates a new SingleBinaryHome.
   *
   * @param storage The directory where the Single Binary File are stored.
   */
  public SingleFileHome(File storage) {
    this._storage = storage;
    LOGGER.debug("Instantiating home for single binary with "+storage);
    if (storage == null)
      LOGGER.warn("The files directory is not setup properly!");
  }

  /**
   * Returns the list of publication currently in the publications' directory.
   * 
   * @param filename The string file name.
   * @param patternMatch The pattern match string to check before creating the file.
   * 
   * @return A list of <code>Publication</code> instances 
   */
  public SingleFile get(String filename, String patternMatch) {
    if (this._storage == null) return null;
    File singlebinary = new File(this._storage, filename);
    SingleFile binary = SingleFile.make(singlebinary, patternMatch);
    binary.setPath("/files" + filename);
    return binary;
  }
  
  /**
   * Returns the list of publication currently in the publications' directory.
   * If this is not necessary, please use SingleFile get (filename, patternMatch)
   * @param filename The string file name.
   * 
   * @return A list of <code>Publication</code> instances 
   */
  public SingleFile get(String filename) {
    if (this._storage == null) return null;
    File singlebinary = new File(this._storage, filename);
    SingleFile binary = SingleFile.make(singlebinary);
    binary.setPath("/files" + filename);
    return binary;
  }	 
}
