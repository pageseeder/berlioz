/*
 * Copyright 2026 Allette Systems (Australia)
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
package org.pageseeder.berlioz.servlet;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Collections;
import org.pageseeder.berlioz.output.OutputWriter;
import org.pageseeder.berlioz.util.SHA256;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.stream.Collectors;

/**
 * Immutable allowlisted servlet diagnostic content. Contains no servlet references or raw settings.
 *
 * @author Christophe Lauret
 * @since 0.14.3
 */
public final class ConfigurationSnapshot {
  private final String root;
  private final List<Map<String, Object>> servlets;

  ConfigurationSnapshot(String root, List<Map<String, Object>> servlets) {
    this.root = root;
    this.servlets = servlets.stream()
        .map(ConfigurationSnapshot::freeze)
        .collect(Collectors.toUnmodifiableList());
  }

  /**
   * Writes the stable XML/JSON schema.
   * @param out destination
   */
  public void writeTo(OutputWriter out) {
    out.startObject(this.root);
    out.startArray("servlets", OutputWriter.ContextOption.JSON_ONLY);
    for (Map<String, Object> servlet : this.servlets) {
      out.startObject("servlet");
      writeFields(out, servlet);
      out.endObject();
    }
    out.endArray();
    out.endObject();
  }

  /** @return unquoted SHA-256 fingerprint of the versioned safe content */
  public String etag() {
    try {
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      DataOutputStream data = new DataOutputStream(bytes);
      canonical(data, "configuration-v1");
      canonical(data, this.root);
      canonical(data, this.servlets);
      return SHA256.hash(bytes.toByteArray());
    } catch (IOException ex) {
      throw new IllegalStateException("Cannot fingerprint configuration", ex);
    }
  }

  private static void canonical(DataOutputStream out, Object value) throws IOException {
    if (value instanceof Map) {
      out.writeByte('o');
      Map<?, ?> fields = (Map<?, ?>) value;
      out.writeInt(fields.size());
      for (Map.Entry<?, ?> field : fields.entrySet()) {
        canonical(out, field.getKey());
        canonical(out, field.getValue());
      }
    } else if (value instanceof List) {
      out.writeByte('a');
      out.writeInt(((List<?>) value).size());
      for (Object item : (List<?>) value) canonical(out, item);
    } else if (value instanceof Boolean) {
      out.writeByte('b');
      out.writeBoolean((Boolean) value);
    } else {
      out.writeByte('s');
      byte[] text = ((String) value).getBytes(StandardCharsets.UTF_8);
      out.writeInt(text.length);
      out.write(text);
    }
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> freeze(Map<String, Object> values) {
    Map<String, Object> copy = new LinkedHashMap<>();
    values.forEach((key, value) -> copy.put(key, value instanceof Map
        ? freeze((Map<String, Object>) value) : value instanceof List ? List.copyOf((List<?>) value) : value));
    return Collections.unmodifiableMap(copy);
  }

  @SuppressWarnings("unchecked")
  private static void writeFields(OutputWriter out, Map<String, Object> fields) {
    fields.forEach((name, value) -> {
      if (value instanceof Map) {
        out.startObject(name);
        writeFields(out, (Map<String, Object>) value);
        out.endObject();
      } else if (value instanceof Boolean) out.field(name, (Boolean) value);
      else if (value instanceof List) out.field(name, (List<String>) value);
      else out.field(name, (String) value);
    });
  }

}
