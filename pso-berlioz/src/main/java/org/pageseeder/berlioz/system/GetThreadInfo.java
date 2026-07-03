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
import java.lang.management.ThreadInfo;

import org.pageseeder.berlioz.Beta;
import org.pageseeder.berlioz.content.ContentStatus;
import org.pageseeder.berlioz.content.Request;
import org.pageseeder.berlioz.content.Response;
import org.pageseeder.berlioz.content.XmlGenerator;
import org.pageseeder.berlioz.xml.XmlWriter;

/**
 * Returns information about a thread.
 *
 * @author Christophe Lauret
 *
 * @version 0.14.0
 * @since 0.9.32
 */
@Beta
public final class GetThreadInfo implements XmlGenerator {

  @Override
  public Response generate(Request req, XmlWriter xml) {
    long threadId = req.parameter("id").asLong().defaultValue(-1L);
    if (threadId < 0) {
      xml.comment("Interval must be strictly positive");
      return Response.status(ContentStatus.BAD_REQUEST);
    } else {
      threadId = Thread.currentThread().getId();
    }

    ThreadInfo thread = Threads.getThreadInfo(threadId);
    if (thread != null) {
      toXML(thread, xml);
    } else {
      xml.openElement("no-thread", true);
      xml.attribute("id", threadId);
      xml.closeElement();
    }
    return Response.ok();
  }

  /**
   * Return all the threads with stack traces
   *
   * @param thread The thread information to serialize as XML
   * @param xml The XML writer
   */
  private static void toXML(ThreadInfo thread, XmlWriter xml) {
    xml.openElement("thread", true);
    xml.attribute("id", thread.getThreadId());
    xml.attribute("name", thread.getThreadName());
    xml.attribute("priority", thread.getPriority());
    xml.attribute("state", thread.getThreadState().name());
    xml.attribute("alive", true);
    xml.attribute("daemon", thread.isDaemon());
    xml.attribute("group", Threads.threadGroupName());

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
