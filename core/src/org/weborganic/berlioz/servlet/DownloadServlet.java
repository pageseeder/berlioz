/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at 
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.servlet;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.weborganic.berlioz.GlobalSettings;
import org.weborganic.berlioz.ReleaseProperties;
import org.weborganic.berlioz.logging.ZLogger;
import org.weborganic.berlioz.logging.ZLoggerFactory;

/**
 * Helper to find downloadable files in the website.
 * 
 * This servlet will simply redirect the request towards the correct file depending on:
 * <ul>
 *   <li>the date</li>
 *   <li>the file type</li>
 *   <li>path info</li>
 * </ul>
 * 
 * @author Christophe Lauret (Allette Systems)
 * @version 28 February 2007
 */
public final class DownloadServlet extends HttpServlet {

  /**
   * As per requirement for the Serializable interface.
   */
  private static final long serialVersionUID = 2007012226180001L;

  /**
   * The date format for the 'date' parameter
   */
  private static final DateFormat ISODATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

  /**
   * Displays debug information.
   */
  private static final ZLogger LOGGER = ZLoggerFactory.getLogger(DownloadServlet.class);

// servlet methods --------------------------------------------------------------------------------

  /**
   * Handles a GET request.
   * 
   * @param req The servlet request.
   * @param res The servlet response.
   * 
   * @throws IOException Should an I/O error occur.
   */
  public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {

    String pathInfo = req.getPathInfo();
    String reqContextPath = req.getContextPath();
    String reqFileName = "".equals(pathInfo)? "" : pathInfo.substring(1);
    String reqFileType = req.getParameter("type");
    ServletContext context = getServletContext();
    String absContextPath = context.getRealPath("/");      
    
    String webpath = getFileWebPath(pathInfo, reqContextPath, reqFileName,
        reqFileType, absContextPath);

    // send redirect to the file
    res.sendRedirect(webpath);

  }

  /**
   * Get the file web path given some request parameters
   * 
   * @param pathInfo
   * @param reqContextPath The request context path (web path).
   * @param reqFilename
   * @param reqFileType
   * @param absContextPath The absolute context path to the file (file system path).
   * @return
   */
  private String getFileWebPath(String pathInfo, String reqContextPath,
      String reqFilename, String reqFileType, String absContextPath) {
    String date = ISODATE_FORMAT.format(ReleaseProperties.getEffectiveDate());
    String filename = toFilename(reqFilename, reqFileType);

    // folder 
    String folder = reqFilename.equals("general-schedule")? "publications/"+ date.substring(0, 4) : "downloads";

    // required because of the staging server  
    File contextPathFile = new File(absContextPath);
    String path = contextPathFile+"/"+folder+pathInfo;
    if (path != null) {
      File filepath = new File(path);
      if (filepath.exists())
        filename = "".equals(pathInfo)? "" : pathInfo.substring(1);
    }

    // Determine the path
    String webpath = reqContextPath + "/"+folder+"/"+filename;
    LOGGER.info("Redirecting the download to "+webpath);

    return webpath;
  }

  /**
   * Returns the exact file name of a downloadable file, given its name 
   * and the type of the file. Note that the effective date is used as a prefix for
   * all files.
   * 
   * @param name The name of the downloadable file.
   * @param type The requested type of the download file.
   * 
   * @return The file extension corresponding to the given type with the 
   * effective date as a prefix.
   */
  public static String toFilename(String name, String type) {
    String date = ISODATE_FORMAT.format(ReleaseProperties.getEffectiveDate());
    
    // get the name of the downloadable, use the name itself by default
    String prefix = GlobalSettings.get("downloads."+name, name);
    Properties p = GlobalSettings.getNode("downloads."+name);
    String extension = p.getProperty(type != null? type : "default", ".zip");
    return date + "-" + prefix + extension;
  }

}
