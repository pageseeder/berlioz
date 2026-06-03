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

import java.io.*;
import java.net.URI;

import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamResult;

/**
 * A Result implementation automatically writing out JSON.
 *
 *
 * @see <a href="http://tools.ietf.org/html/rfc4627">The application/json Media Type for
 *  JavaScript Object Notation (JSON)</a>
 *
 * @author Christophe Lauret
 *
 * @version 0.13.0
 * @since 0.9.32
 */
public class JSONResult extends SAXResult implements Result {

  /**
   * Zero-argument default constructor.
   *
   * <p>transformation results will go to <code>System.out</code>.
   */
  public JSONResult() {
    super(new JSONSerializer());
  }

  /**
   * Construct a JSONResult from a byte stream.
   *
   * @param out A valid OutputStream.
   */
  public JSONResult(OutputStream out) {
    super(new JSONSerializer(out));
  }

  /**
   * Construct a JSONResult from a character stream.
   *
   * <p>It is generally preferable to use a byte stream so that the encoding can controlled by the
   * <code>xsl:output</code> declaration; but can be convenient when using StringWriter
   *
   * @param writer A valid character stream.
   */
  public JSONResult(Writer writer) {
    super(new JSONSerializer(writer));
  }

  // Static helpers
  // ---------------------------------------------------------------------------------------------

  /**
   * Returns a new instance of the XSLT result if applicable.
   *
   * @param t      The XSLT transformer
   * @param result The result of transformation as a stream
   *
   * @return A new XSLT result if applicable.
   */
  public static Result newInstanceIfSupported(Transformer t, StreamResult result) {
    return supports(t)? newInstance(result) : result;
  }

  /**
   * Returns a new instance from the specified stream result.
   *
   * @param result a non-null stream result instance.
   *
   * @return a new <code>JSONResult</code> instance using the same properties as the stream result.
   *
   * @throws NullPointerException If the result is stream is <code>null</code>
   */
  public static JSONResult newInstance(StreamResult result) {
    String systemId = result.getSystemId();
    JSONResult json = createJSONResult(result);
    if (systemId != null) json.setSystemId(systemId);
    return json;
  }

  private static JSONResult createJSONResult(StreamResult result) {
    OutputStream out = result.getOutputStream();
    if (out != null) return new JSONResult(out);
    Writer writer = result.getWriter();
    if (writer != null) return new JSONResult(writer);
    String systemId = result.getSystemId();
    if (systemId != null) return newInstanceFromSystemId(systemId);
    return new JSONResult();
  }

  private static JSONResult newInstanceFromSystemId(String systemId) {
    try {
      // URI.create() and new File(URI) both throw IllegalArgumentException for
      // non-URI system IDs (e.g. a plain file path like "/tmp/out.json").
      return new JSONResult(new FileOutputStream(new File(URI.create(systemId))));
    } catch (IOException ex) {
      throw new UncheckedIOException("Unable to write JSON to " + systemId, ex);
    } catch (IllegalArgumentException ex) {
      throw new IllegalArgumentException(
          "System ID must be a valid file URI (e.g. file:///path/to/out.json), got: " + systemId, ex);
    }
  }

  /**
   * Indicates whether the specified transformer is supported based on its output properties.
   *
   * <p>The transformer is considered to support this Result type if it uses the "xml" method and
   * specifies the media type as "application/json".
   *
   * @param t the XSLT transformer implementation
   *
   * @return <code>true</code> if it matches the conditions above;
   *         <code>false</code> otherwise.
   */
  public static boolean supports(Transformer t) {
    String method = t.getOutputProperty("method");
    String media = t.getOutputProperty("media-type");
    return "xml".equals(method) && "application/json".equals(media);
  }

}
