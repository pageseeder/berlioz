package org.weborganic.berlioz.document;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import com.topologi.diffx.xml.XMLWritable;
import com.topologi.diffx.xml.XMLWriter;

/**
 * A static page object.
 * 
 * @author Christophe Lauret (Allette Systems)
 * 
 * @version 16 August 2007
 */
public final class StaticPage implements XMLWritable, MetadataHolder {

  /**
   * Maps files to static pages instances.
   */
  private static final Hashtable STATIC_PAGES = new Hashtable();

  /**
   * Filter for the year directory.
   */
  private static final FileFilter PAGE_FILE_FILTER = new FileFilter() {
    public boolean accept(File file) {
      return (file.getName().endsWith(".xml"));
    }
  };

  /**
   * The file representing the static page
   */
  private final File _file;

  /**
   * Indicates whether the metadata has been loaded.
   */
  private boolean _loaded;

  /**
   * The title of this page.
   */
  private String _title;

  /**
   * The description of this page.
   */
  private String _description;

  /**
   * Creates a new static page from the specified file.
   * 
   * @param file The XML file.
   */
  private StaticPage(File file) {
    this._file = file;
    this._title = file.getName();
    this._description = null;
  }

  /**
   * Returns the simple name of this page (different from title).
   * 
   * @return The name of the static page.
   */
  public String getName() {
    String filename = this._file.getName();
    int dot = filename.indexOf('.');
    return (dot > 0)? filename.substring(0, dot) : filename;
  }

  /**
   * Returns the folder containing this file.
   * 
   * @return The folder containing this file.
   */
  public String getFolder() {
    String fullpath = this._file.getParentFile().getAbsolutePath().replace('\\', '/');
    int statik = fullpath.indexOf("static/");
    if (statik >= 0) {
      int slash  = fullpath.indexOf("/", statik);
      if (slash >= 0) {
        return fullpath.substring(slash);
      } else return "";
    } else return "";
  }

  /**
   * {@inheritDoc}
   */
  public String getTitle() {
    if (!this._loaded)
      this._loaded = MetadataHandler.load(this, this._file);
    return this._title;
  }

  /**
   * {@inheritDoc}
   */
  public String getDescription() {
    if (!this._loaded)
      this._loaded = MetadataHandler.load(this, this._file);
    return this._description;
  }

  /**
   * Returns the title of the static page.
   * 
   * @return The title of the static page.
   */
  public StaticPage getParent() {
    File parent = this._file.getParentFile();
    File ancestor = parent.getParentFile();
    return StaticPage.make(new File(ancestor, parent.getName()+".xml"));
  }

  /**
   * Returns the list of siblings for this static page.
   * 
   * @return The title of the static page.
   */
  public List listSiblings() {
    List siblings = new ArrayList();
    File parent = this._file.getParentFile();
    File[] files = parent.listFiles(PAGE_FILE_FILTER);
    // iterates over the files in the directory
    for (int i = 0; i < files.length; i++) {
      if (!files[i].getName().equals(this._file.getName()))
        siblings.add(StaticPage.make(files[i]));
    }
    return siblings;
  }

  /**
   * Returns the list of siblings for this static page.
   * 
   * @return The title of the static page.
   */
  public List listChildren() {
    List children = new ArrayList();
    File parent = new File(this._file.getParentFile(), this.getName());
    if (parent.isDirectory()) {
      File[] files = parent.listFiles(PAGE_FILE_FILTER);
      // iterates over the files in the directory
      for (int i = 0; i < files.length; i++) {
        children.add(StaticPage.make(files[i]));
      }
    }
    return children;
  }

  /**
   * {@inheritDoc}
   */
  public void toXML(XMLWriter xml) throws IOException {
    xml.openElement("static-page", true);
    // TODO clean up
    xml.attribute("folder", this.getFolder());
    xml.attribute("name", this.getName());
    xml.element("title", this.getTitle());
    xml.element("description", this.getDescription());
    xml.closeElement(); // 'static-page'
  }

  // protected methods ----------------------------------------------------------------------------

  /**
   * Sets the title for this static page.
   * 
   * @param title The title for this page.
   */
  public void setTitle(String title) {
    this._title = title;
  }

  /**
   * Sets the description for this static page.
   * 
   * @param description The description for this page.
   */
  public void setDescription(String description) {
    this._description = description;
  }

  // static helpers -------------------------------------------------------------------------------

  /**
   * Create a static page instance. 
   * 
   * @param file The file to use to create the static page.
   * 
   * @return The corresponding static page instance or <code>null</code>.
   */
  public static StaticPage make(File file) {
    if (file == null) return null;
    if (!file.exists()) return null;
    StaticPage page = (StaticPage)STATIC_PAGES.get(file);
    if (page == null) {
      page = new StaticPage(file);
      STATIC_PAGES.put(file, page);
    }
    return page;
  }

  /**
   * Returns the name of 
   * 
   * @param file The file corresponding to the fact sheet.
   * 
   * @return The name of the fact sheet.
   */
  public static String toName(File file) {
    if (file == null) return null;
    String filename = file.getName();
    int dot = filename.indexOf('.');
    return (dot > 0)? filename.substring(0, dot) : filename;
  }

}
