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
import java.lang.management.ThreadMXBean;

import org.jspecify.annotations.Nullable;
import org.pageseeder.berlioz.Beta;
import org.pageseeder.berlioz.content.ContentGenerator;
import org.pageseeder.berlioz.content.ContentRequest;
import org.pageseeder.xmlwriter.XMLWriter;

/**
 * Returns information about the threads running in the system.
 *
 * <ul>
 *   <li><code>NEW</code>. The thread has been created, but hasn't run yet.</li>
 *   <li><code>TERMINATED</code>. The thread has run to completion, but hasn't been deleted yet by the JVM.</li>
 *   <li><code>RUNNABLE</code>. The thread is running.</li>
 *   <li><code>BLOCKED</code>. The thread is blocked waiting on a lock (such as in a synchronized block or method).</li>
 *   <li><code>WAITING</code>. The thread is waiting until another thread calls notify().</li>
 *   <li><code>TIMED_WAITING</code>. The thread is either waiting or in a sleep().</li>
 * </ul>
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.9.32
 * @since Berlioz 0.9.32
 */
@Beta
public final class ListThreads implements ContentGenerator {

  @Override
  public void process(ContentRequest req, XMLWriter xml) throws IOException {

    boolean stackTraces = "true".equals(req.getParameter("stacktraces"));
    boolean threadTime = "true".equals(req.getParameter("threadtime"));

    ThreadMXBean threadBean = Threads.getThreadMXBean();
    ThreadMXBean bean = threadTime? threadBean : null;
    if (bean != null && !bean.isThreadCpuTimeSupported()) {
      bean = null;
    }

    xml.openElement("threads");
    ThreadInfo[] threads = Threads.getThreadInfo(threadBean, stackTraces? Integer.MAX_VALUE : 0);
    for (ThreadInfo thread : threads) {
      if (thread != null) {
        if (stackTraces) {
          toXML(thread, thread.getStackTrace(), bean, xml);
        } else {
          toXML(thread, bean, xml);
        }
      }
    }
    xml.closeElement();
  }

  /**
   * Display the specified thread.
   *
   * @param thread The thread to serialise as XML
   * @param bean  The management bean for CPU time (may be <code>null</code>)
   * @param xml   The XML Writer
   *
   * @throws IOException Should an error occur while writing the XML
   */
  private static void toXML(ThreadInfo thread, @Nullable ThreadMXBean bean, XMLWriter xml) throws IOException {
    xml.openElement("thread", true);
    writeThreadAttributes(thread, xml);

    if (bean != null) {
      final long cpu = bean.getThreadCpuTime(thread.getThreadId());
      final long user = bean.getThreadUserTime(thread.getThreadId());
      xml.openElement("times");
      xml.attribute("cpu", Long.toString(cpu));
      xml.attribute("user", Long.toString(user));
      xml.attribute("system", Long.toString(cpu - user));
      xml.closeElement();
    }

    xml.closeElement();
  }

  /**
   * Return all the threads with stack traces
   *
   * @param thread     The thread information to serialise as XML
   * @param stacktrace The stack trace (may be <code>null</code>)
   * @param bean       The management bean for CPU time (may be <code>null</code>)
   * @param xml The XML writer
   *
   * @throws IOException If thrown while writing XML.
   */
  private static void toXML(ThreadInfo thread, StackTraceElement[] stacktrace, @Nullable ThreadMXBean bean, XMLWriter xml)
      throws IOException {
    xml.openElement("thread", true);
    writeThreadAttributes(thread, xml);

    // If the management bean is available include times
    if (bean != null) {
      final long cpu = bean.getThreadCpuTime(thread.getThreadId());
      final long user = bean.getThreadUserTime(thread.getThreadId());
      xml.openElement("time");
      xml.attribute("cpu", Long.toString(cpu));
      xml.attribute("user", Long.toString(user));
      xml.attribute("system", Long.toString(cpu - user));
      xml.closeElement();
    }

    // If the stack trace is enabled
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

  private static void writeThreadAttributes(ThreadInfo thread, XMLWriter xml) throws IOException {
    xml.attribute("id", Long.toString(thread.getThreadId()));
    xml.attribute("name", thread.getThreadName());
    xml.attribute("priority", thread.getPriority());
    xml.attribute("state", thread.getThreadState().name());
    xml.attribute("alive", Boolean.TRUE.toString());
    xml.attribute("daemon", Boolean.toString(thread.isDaemon()));
    xml.attribute("group", Threads.threadGroupName());
    if (thread.getThreadId() == Thread.currentThread().getId()) {
      // Flag the current thread
      xml.attribute("current", "true");
    }
  }

}
