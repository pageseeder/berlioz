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
package org.pageseeder.berlioz.error;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.pageseeder.berlioz.content.ContentStatus;
import org.pageseeder.berlioz.util.CollectedError;
import org.pageseeder.berlioz.util.CollectedError.Level;
import org.pageseeder.berlioz.xml.XmlStringBuilder;

import javax.xml.transform.TransformerException;
import java.util.List;

class CollectedErrorsExtensionTest {

  @Test
  void name_isCollectedErrors() {
    CollectedErrorsExtension<TransformerException> extension = new CollectedErrorsExtension<>(List.of());
    Assertions.assertEquals("collected-errors", extension.name());
  }

  @Test
  void toXml_writesOneCollectedElementPerItemWithLevelAndException() {
    List<CollectedError<TransformerException>> items = List.of(
        new CollectedError<>(Level.ERROR, new TransformerException("first failure")),
        new CollectedError<>(Level.WARNING, new TransformerException("second failure")));

    XmlStringBuilder out = new XmlStringBuilder();
    ProblemDetails.of(ContentStatus.INTERNAL_SERVER_ERROR)
        .extension(new CollectedErrorsExtension<>(items))
        .toXml(out);
    String xml = out.toString();

    Assertions.assertTrue(xml.contains("<collected-errors>"), xml);
    Assertions.assertTrue(xml.contains("<collected level=\"error\">"), xml);
    Assertions.assertTrue(xml.contains("<collected level=\"warning\">"), xml);
    Assertions.assertTrue(xml.contains("<exception class=\"javax.xml.transform.TransformerException\" type=\"TransformerException\">"), xml);
    Assertions.assertTrue(xml.contains("<message>first failure</message>"), xml);
    Assertions.assertTrue(xml.contains("<message>second failure</message>"), xml);
    // Standard verbosity only: no stack trace for collected entries
    Assertions.assertFalse(xml.contains("<stack-trace>"), xml);
  }

  @Test
  void toJson_writesCollectedErrorsArray() {
    List<CollectedError<TransformerException>> items = List.of(
        new CollectedError<>(Level.FATAL, new TransformerException("boom")));

    String json = ProblemDetails.of(ContentStatus.INTERNAL_SERVER_ERROR)
        .extension(new CollectedErrorsExtension<>(items))
        .toJson();

    Assertions.assertTrue(json.contains("\"collectedErrors\":["), json);
    Assertions.assertTrue(json.contains("\"level\":\"fatal\""), json);
    Assertions.assertTrue(json.contains("\"exception\":{"), json);
    Assertions.assertTrue(json.contains("\"boom\""), json);
  }

  @Test
  void toXml_emptyListStillWritesEmptyWrapperElement() {
    XmlStringBuilder out = new XmlStringBuilder();
    ProblemDetails.of(ContentStatus.INTERNAL_SERVER_ERROR)
        .extension(new CollectedErrorsExtension<>(List.of()))
        .toXml(out);
    Assertions.assertTrue(out.toString().contains("<collected-errors/>"));
  }

  @Test
  void constructor_rejectsNullList() {
    Assertions.assertThrows(NullPointerException.class, () -> new CollectedErrorsExtension<TransformerException>(null));
  }

}
