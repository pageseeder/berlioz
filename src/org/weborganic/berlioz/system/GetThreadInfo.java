/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.system;

import java.io.IOException;

import org.weborganic.berlioz.BerliozException;
import org.weborganic.berlioz.Beta;
import org.weborganic.berlioz.content.ContentGenerator;
import org.weborganic.berlioz.content.ContentRequest;
import org.weborganic.berlioz.content.ContentStatus;

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
