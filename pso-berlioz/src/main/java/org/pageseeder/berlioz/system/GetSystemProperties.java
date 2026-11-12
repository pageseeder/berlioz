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
package org.pageseeder.berlioz.system;

import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.pageseeder.berlioz.Beta;
import org.pageseeder.berlioz.content.Generator;
import org.pageseeder.berlioz.content.Request;
import org.pageseeder.berlioz.content.Response;
import org.pageseeder.berlioz.output.OutputWriter;
import org.pageseeder.berlioz.util.Redaction;

/**
 * Returns JVM system properties as XML or JSON.
 *
 * <p>Exposes the properties available via {@link System#getProperties()}, sorted by name.
 * Values are redacted with {@link Redaction#redact(String, String)}, as in
 * {@link org.pageseeder.berlioz.generator.GetGlobalConfig}: the whole value when its name matches
 * a sensitive keyword (e.g. {@code password}, {@code secret}, {@code token}, {@code apikey}), and
 * otherwise the password of a URL and the values of parameters with a sensitive name. The program
 * arguments in {@code sun.java.command}, after the main class or JAR, are replaced with
 * {@code [REDACTED]}. Paths and user names are not redacted, so the values still include
 * environment details (e.g. user home directory, OS details) — restrict access to this generator
 * in production environments.
 *
 * <h3>Parameters</h3>
 * <p>This generator does not use any parameter.</p>
 *
 * <h3>Returned XML</h3>
 * <pre>{@code
 * <system>
 *   <property name="[nameA]" value="[valueA]"/>
 *   <property name="[nameB]" value="[REDACTED]"/>
 *   ...
 * </system>
 * }</pre>
 *
 * <h3>Returned JSON</h3>
 * <pre>{@code
 * {
 *   "properties": [
 *     {"name": "[nameA]", "value": "[valueA]"},
 *     {"name": "[nameB]", "value": "[REDACTED]"}
 *   ]
 * }
 * }</pre>
 *
 * <h3>Usage</h3>
 * <p>To use this generator in Berlioz (in <code>/WEB-INF/config/services.xml</code>):
 * <pre>{@code <generator class="org.pageseeder.berlioz.system.GetSystemProperties"
 *                         name="[name]" target="[target]"/>}</pre>
 *
 * @author Christophe Lauret
 *
 * @version 0.14.6
 * @since 0.9.32
 */
@Beta
public final class GetSystemProperties implements Generator {

  /** The property with the main class or JAR of the JVM, followed by the program arguments. */
  static final String JAVA_COMMAND = "sun.java.command";

  /** The end of the JAR path in the command: '.jar' followed by whitespace or the end. */
  private static final Pattern JAR_END = Pattern.compile("\\.jar(?=\\s|$)");

  /** A main class, or a module and main class ({@code java -m}): never a path with spaces. */
  private static final Pattern MAIN_CLASS =
      Pattern.compile("[\\p{javaJavaIdentifierPart}.]+(?:/[\\p{javaJavaIdentifierPart}.]+)?");

  @Override
  public Response generate(Request req, OutputWriter out) {
    Properties properties = System.getProperties();
    // Only string entries: other entries could not be serialized as values anyway
    Set<String> names = new TreeSet<>(properties.stringPropertyNames());

    out.startObject("system");
    out.startArray("properties", OutputWriter.ContextOption.JSON_ONLY);
    for (String name : names) {
      String value = properties.getProperty(name);
      if (value == null) continue; // removed concurrently
      out.startObject("property");
      out.field("name", name);
      out.field("value", JAVA_COMMAND.equals(name) ? redactCommand(value) : Redaction.redact(name, value));
      out.endObject();
    }
    out.endArray();
    out.endObject();
    return Response.ok();
  }

  /**
   * @param command the value of {@code sun.java.command}: the main class or the path of a JAR,
   *                then the program arguments, separated by whitespace
   * @return the main class or JAR, followed by {@code [REDACTED]} when there are arguments
   */
  static String redactCommand(String command) {
    String value = command.strip();
    int end = executableEnd(value);
    if (end < 0 || value.substring(end).isBlank()) return Redaction.redactValue(value);
    return Redaction.redactValue(value.substring(0, end)) + ' ' + Redaction.REDACTED;
  }

  /**
   * Finds the end of the main class or JAR, which comes first in the command.
   *
   * <p>A main class cannot contain whitespace, so when the first word is a class name, the
   * arguments start after it, even if one of them is a JAR. Otherwise, the command starts with
   * the path of a JAR, which can contain spaces, up to the first {@code .jar}. In doubt, the
   * command ends at the first whitespace: a relative JAR path with spaces is truncated, so that
   * an argument is never shown.
   *
   * @return the index after the main class or JAR, or -1 if the command has no whitespace
   */
  private static int executableEnd(String value) {
    int space = firstWhitespace(value);
    if (space < 0) return -1;
    if (MAIN_CLASS.matcher(value.substring(0, space)).matches()) return space;
    Matcher jar = JAR_END.matcher(value.toLowerCase(Locale.ROOT));
    return jar.find() ? jar.end() : space;
  }

  private static int firstWhitespace(String value) {
    for (int i = 0; i < value.length(); i++) {
      if (Character.isWhitespace(value.charAt(i))) return i;
    }
    return -1;
  }

}
