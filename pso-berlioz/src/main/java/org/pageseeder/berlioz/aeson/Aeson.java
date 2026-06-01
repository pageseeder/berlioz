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
package org.pageseeder.berlioz.aeson;

import java.io.File;

import javax.xml.XMLConstants;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.jspecify.annotations.Nullable;

/**
 * Contains logic to invoke this library on the command-line.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.12.3
 * @since Berlioz 0.9.32
 */
@SuppressWarnings("java:S106") // CLI entry point — System.out/err are correct here
public final class Aeson {

  /**
   * To invoke this library on the command line.
   *
   * <p>The options are as follows:
   * <pre>
   * -s:[source]       File or directory containing files to process (XML)
   * -xsl:[stylesheet] Stylesheet to process the files (XSLT)
   * -o:[output]       File or directory receiving transformation results (optional if source is file)
   * </pre>
   *
   * @param args command-line arguments
   * @throws TransformerException should anything go wrong.
   */
  public static void main(String[] args) throws TransformerException {
    File source = getFile(args, "-s:");
    File style  = getFile(args, "-xsl:");
    File output = getFile(args, "-o:");

    if (source == null || !source.exists()) {
      System.err.println("Unable to process source: " + source);
      System.exit(0);
      return;
    }
    if (source.isDirectory() && (output == null || output.isFile())) {
      System.err.println("When source is a directory, the output must be specified and be a directory");
      System.exit(0);
      return;
    }

    Transformer transformer = setupTransformer(style);
    if (source.isDirectory()) processDirectory(source, output, transformer);
    else                      processFile(source, output, transformer);
  }

  private static Transformer setupTransformer(@Nullable File style) throws TransformerConfigurationException {
    TransformerFactory factory = TransformerFactory.newInstance();
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
    if (style != null) {
      return factory.newTransformer(new StreamSource(style));
    }
    Transformer transformer = factory.newTransformer();
    transformer.setOutputProperty("method", "xml");
    transformer.setOutputProperty("media-type", "application/json");
    return transformer;
  }

  private static void processDirectory(File source, @Nullable File output, Transformer transformer) throws TransformerException {
    if (output != null && !output.exists() && !output.mkdirs()) {
      System.err.println("Unable to create output directory: " + output);
      System.exit(0);
      return;
    }
    File[] files = source.listFiles(); // listFiles() returns null only if source is not a directory or an I/O error occurs
    if (files == null) {
      System.err.println("Unable to list source files");
      System.exit(0);
      return;
    }
    for (File f : files) {
      StreamResult r = new StreamResult(new File(output, toOutputName(f.getName(), transformer)));
      transformer.transform(new StreamSource(f), JSONResult.newInstanceIfSupported(transformer, r));
    }
  }

  private static void processFile(File source, @Nullable File output, Transformer transformer) throws TransformerException {
    StreamResult r = output != null ? new StreamResult(output) : new StreamResult(System.out);
    transformer.transform(new StreamSource(source), JSONResult.newInstanceIfSupported(transformer, r));
  }

  /**
   * Returns a file from a command-line argument by prefix
   *
   * @param args   the array of command-line arguments
   * @param prefix the prefix to look for
   *
   * @return the file corresponding to a matching argument without the prefix or <code>null</code>
   */
  private static @Nullable File getFile(String[] args, String prefix) {
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
  private static @Nullable String getByPrefix(String[] args, String prefix) {
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
