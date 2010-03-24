package org.weborganic.berlioz.document;

import java.io.File;

import org.weborganic.berlioz.xml.XMLUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;


/**
 * Loads the metadata from the given metadata holder.
 * 
 * <ul>
 *   <li>The title is loaded from the first 'title' element.</li>
 *   <li>The description is loaded from the first 'para' element.</li>
 * </ul>
 * 
 * @author Christophe Lauret (Allette Systems)
 * @version 16 August 2007
 */
public final class MetadataHandler extends DefaultHandler {

  /**
   * The static page page which metadata is being loaded.
   */
  private final MetadataHolder _holder;

  /**
   * The text buffer for extracting character data.
   */
  private final StringBuffer buffer = new StringBuffer();

  /**
   * Indicates whether the characters method should be recording character data.
   */
  private boolean isRecording;

  /**
   * State variable; indicates whether this is the first title.
   */
  private boolean isFirstTitle;

  /**
   * State variable; indicates whether this is the first description.
   */
  private boolean isFirstDescription;

  /**
   * Local element attributes
   */
  private Attributes attribute;
  
  /**
   * Creates a new handler for the specified holder.
   * 
   * @param holder The static page page which metadata is being loaded.
   */
  public MetadataHandler(MetadataHolder holder) {
    this._holder = holder;
    this.isFirstTitle = true;
    this.isFirstDescription = true;
  }

  /**
   * {@inheritDoc}
   */
  public void startElement(String uri, String localName, String qName, Attributes atts)
      throws SAXException {
	attribute = atts;  
    if ("title".equals(localName) && this.isFirstTitle) {
      this.isRecording = true;
      this.buffer.setLength(0);
    } else if ("paraLabel".equals(localName) && "description".equals(atts.getValue("name")) && this.isFirstDescription) {
        this.isRecording = true;
        this.buffer.setLength(0);
    } else if ("inlineLabel".equals(localName) && "description".equals(atts.getValue("name")) && this.isFirstDescription) {
        this.isRecording = true;
        this.buffer.setLength(0);
    }
  }

  /**
   * {@inheritDoc}
   */
  public void characters(char[] ch, int start, int length) throws SAXException {
    if (this.isRecording) {
      this.buffer.append(ch, start, length);
    }
  }

  /**
   * {@inheritDoc}
   */
  public void endElement(String uri, String localName, String qName) throws SAXException {
    // set the title
    if ("title".equals(localName) && this.isFirstTitle) {
      this._holder.setTitle(this.buffer.toString());
      this.isFirstTitle = false;
      // set the description
    } else if ("paraLabel".equals(localName) && "description".equals(attribute.getValue("name")) && this.isFirstDescription) {
        this._holder.setDescription(this.buffer.toString());
        this.isFirstDescription = false;
    } else if ("inlineLabel".equals(localName) && "description".equals(attribute.getValue("name")) && this.isFirstDescription) {
        this._holder.setDescription(this.buffer.toString());
        this.isFirstDescription = false;
    }
  }

// static helpers ==================================================================================

  /**
   * Parses the file and populates the metadata.
   * 
   * @param holder The metadata instance to load.
   * @param file   The XML file to utilise.
   * 
   * @return <code>true</code> if the metadata was loaded properly;
   *         <code>false</code> otherwise (if an error occurred);
   */
  public static boolean load(MetadataHolder holder, File file) {
    try {
      XMLUtils.parse(new MetadataHandler(holder), file);
    } catch (Exception ex) {
      ex.printStackTrace();
      return false;
    }
    return true;
  }
  
}
