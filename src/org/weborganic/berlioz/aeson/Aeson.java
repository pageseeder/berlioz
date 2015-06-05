/*
 * This file is part of the Aeson library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.aeson;

import java.io.File;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

/**
 * Contains logic to invoke this library on the command-line.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.9.32
 * @since Berlioz 0.9.32
 */
public class Aeson {

  /**
   * To invoke this library on the command line.
   *
   * The options are as follows:
   * <pre>
   * -s:[source]       File or directory containing files to process (XML)
   * -xsl:[stylesheet] Stylesheet to process the files (XSLT)
   * -o:[output]       File or directory receiving transformation results (optional if source is file)
   * </pre>
   *
   * @param args command-line arguments
   * @throws Exception should anything go wrong.
   */
  public static void main(String[] args) throws Exception {

    // Grab arguments
    File source = getFile(args, "-s:");
    File style = getFile(args, "-xsl:");
    File output = getFile(args, "-o:");

    // Source is required
    if (source == null || !source.exists()) {
      System.err.println("Unable to process source: "+source);
      System.exit(0);
    }

    // Output folder required if source is a folder
    if (source.isDirectory()) {
      if (output == null || output.isFile()) {
        System.err.println("When source is a directory, the output must be specified and be a directory");
        System.exit(0);
      }
    }

    // Setup the transformer
    TransformerFactory factory = TransformerFactory.newInstance();
    Transformer transformer = null;
    if (style != null) {
      Source xslt = new StreamSource(style);
      transformer = factory.newTransformer(xslt);
    } else {
      // This should create a identity transformer
      // TODO: We could simply SAX parse the xml...
      transformer = factory.newTransformer();
      transformer.setOutputProperty("method", "xml");
      transformer.setOutputProperty("media-type", "application/json");
    }

    // Process
    if (source.isDirectory()) {

      // Let's ensure the output dir exists
      if (!output.exists()) {
        output.mkdirs();
      }

      // Iterate over files in directory
      for (File f : source.listFiles()) {
        StreamSource s = new StreamSource(f);
        StreamResult r = new StreamResult(new File(output, toOutputName(f.getName(), transformer)));
        Result result = JSONResult.newInstanceIfSupported(transformer, r);
        transformer.transform(s, result);
      }

    } else {

      // Process individual file
      StreamSource s = new StreamSource(source);
      StreamResult r;
      if (output != null) {
        r = new StreamResult(output);
      } else {
        r = new StreamResult(System.out);
      }
      Result result = JSONResult.newInstanceIfSupported(transformer, r);
      transformer.transform(s, result);
    }

  }

  /**
   * Returns a file from a command-line argument by prefix
   *
   * @param args   the array of command-line arguments
   * @param prefix the prefix to look for
   *
   * @return the file corresponding to a matching argument without the prefix or <code>null</code>
   */
  private static File getFile(String[] args, String prefix) {
    String value = getByPrefix(args, prefix);
    if (value != null)
      return new File(value);
    else
      return null;
  }

  /**
   * Returns a command-line argument by prefix.
   *
   * @param args   the array of command-line arguments
   * @param prefix the prefix to look for
   *
   * @return the matching argument without the prefix or <code>null</code>
   */
  private static String getByPrefix(String[] args, String prefix) {
    for (String arg : args) {
      if (arg.startsWith(prefix))
        return arg.substring(prefix.length());
    }
    return null;
  }

  /**
   * Compute the name of the file to output based on the method and media type
   * of the transformer.
   *
   * @param name        The name of the file to transform.
   * @param transformer The transformer in use
   *
   * @return The corresponding output name.
   */
  private static String toOutputName(String name, Transformer transformer) {
    String method = transformer.getOutputProperty("method");
    String media = transformer.getOutputProperty("media-type");
    int dot = name.lastIndexOf('.');
    String withoutExt = dot >= 0? name.substring(0, dot) : name;
    if ("xml".equals(method)) {
      if ("application/json".equals(media)) return withoutExt+".json";
      else return withoutExt+".xml";
    } else if ("html".equals(method)) return withoutExt+".html";
    else if ("text".equals(method)) return withoutExt+".txt";
    else return name;
  }
}
