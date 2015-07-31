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

import java.io.IOException;

import org.pageseeder.berlioz.BerliozException;
import org.pageseeder.berlioz.Beta;
import org.pageseeder.berlioz.content.ContentGenerator;
import org.pageseeder.berlioz.content.ContentRequest;
import org.pageseeder.berlioz.content.ContentStatus;

import com.topologi.diffx.xml.XMLWriter;

/**
 * Returns information about a thread.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.9.32
 * @since Berlioz 0.9.32
 */
@Beta
public final class GetThreadInfo implements ContentGenerator {

  @Override
  public void process(ContentRequest req, XMLWriter xml) throws BerliozException, IOException {

    long threadId = -1;
    String id = req.getParameter("id", "-1");
    if (id != null) {
      try {
        threadId = Long.parseLong(id);
      } catch (NumberFormatException ex) {
        req.setStatus(ContentStatus.BAD_REQUEST);
        xml.writeComment("Interval must be strictly positive");
        return;
      }
    } else {
      threadId = Thread.currentThread().getId();
    }

    Thread thread = Threads.getThread(threadId);
    toXML(thread, xml);
  }

  /**
   * Return all the threads with stack traces
   *
   * @param thread     The thread to serialise as XML
   * @param xml The XML writer
   *
   * @throws IOException If thrown while writing XML.
   */
  private static void toXML(Thread thread, XMLWriter xml)
      throws IOException {
    xml.openElement("thread", true);
    xml.attribute("id", Long.toString(thread.getId()));
    xml.attribute("name", thread.getName());
    xml.attribute("priority", thread.getPriority());
    xml.attribute("state", thread.getState().name());
    xml.attribute("alive", Boolean.toString(thread.isAlive()));
    xml.attribute("daemon", Boolean.toString(thread.isDaemon()));
    xml.attribute("group", thread.getThreadGroup().getName());

    StackTraceElement[] stacktrace = thread.getStackTrace();
    if (stacktrace != null) {
      xml.openElement("stacktrace");
      for (StackTraceElement element : stacktrace) {
        xml.openElement("element");
        String method = element.getMethodName();
        String filename = element.getFileName();
        int line = element.getLineNumber();
        xml.attribute("class", element.getClassName());
        if (filename != null) {
          xml.attribute("filename", filename);
        }
        if (method != null) {
          xml.attribute("method", method);
        }
        if (line >= 0) {
          xml.attribute("line", line);
        }
        xml.closeElement();
      }
      xml.closeElement();
    }

    xml.closeElement();
  }
}
