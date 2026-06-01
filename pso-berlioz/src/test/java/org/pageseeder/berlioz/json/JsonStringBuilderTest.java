/*
 * Copyright 2020 Allette Systems (Australia)
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
package org.pageseeder.berlioz.json;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class JsonStringBuilderTest {

  @Test
  public void testSimpleObject() {
    JsonStringBuilder builder = JsonStringBuilder.create();
    builder.startObject()
        .field("name", "test")
        .field("count", 42L)
        .endObject()
        .flush();
    Assertions.assertEquals(builder.toString(), "{\"name\":\"test\",\"count\":42}");
  }

  @Test
  public void testArray() {
    JsonStringBuilder builder = JsonStringBuilder.create();
    builder.startArray()
        .value("a")
        .value("b")
        .endArray()
        .flush();
    Assertions.assertEquals(builder.toString(), "[\"a\",\"b\"]");
  }

  @Test
  public void testNullNamedField() {
    JsonStringBuilder builder = JsonStringBuilder.create();
    builder.startObject()
        .nullValue("key")
        .endObject()
        .flush();
    Assertions.assertEquals(builder.toString(), "{\"key\":null}");
  }

  @Test
  public void testInObject() {
    JsonStringBuilder builder = JsonStringBuilder.create();
    Assertions.assertFalse(builder.inObject());
    builder.startObject();
    Assertions.assertTrue(builder.inObject());
    builder.endObject().flush();
  }

  @Test
  public void testCloseFlushesContent() {
    JsonStringBuilder builder = JsonStringBuilder.create();
    builder.startObject().field("x", 1L).endObject();
    builder.close();
    Assertions.assertEquals(builder.toString(), "{\"x\":1}");
  }

}
