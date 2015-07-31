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
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.Map;
import java.util.Map.Entry;

import org.pageseeder.berlioz.BerliozException;
import org.pageseeder.berlioz.Beta;
import org.pageseeder.berlioz.content.ContentGenerator;
import org.pageseeder.berlioz.content.ContentRequest;

import com.topologi.diffx.xml.XMLWriter;

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
  public void process(ContentRequest req, XMLWriter xml) throws BerliozException, IOException {

    boolean stacktraces = "true".equals(req.getParameter("stacktraces"));
    boolean threadtime = "true".equals(req.getParameter("threadtime"));

    ThreadMXBean bean = threadtime? ManagementFactory.getThreadMXBean() : null;
    if (bean != null && !bean.isThreadCpuTimeSupported()) {
      bean = null;
    }

    xml.openElement("threads");

    if (stacktraces) {
      // Use slow but convenient method to load the threads with their stack traces
      Map<Thread, StackTraceElement[]> all = Thread.getAllStackTraces();
      for (Entry<Thread, StackTraceElement[]> e : all.entrySet()) {
        toXML(e.getKey(), e.getValue(), bean, xml);
      }

    } else {
      // Use old-school method
      ThreadGroup root = Threads.getRootThreadGroup();
      toXML(root, bean, xml);
    }

    xml.closeElement();
  }

  /**
   * Display the specified group thread.
   *
   * @param group The group thread to display as a tree
   * @param bean  The management bean for CPU time (may be <code>null</code>)
   * @param xml   The XML Writer
   *
   * @throws IOException Should an error occur while writing the XML
   */
  private static void toXML(ThreadGroup group, ThreadMXBean bean, XMLWriter xml) throws IOException {

    // Grab all the threads
    Thread[] threads = new Thread[group.activeCount()];
    while (group.enumerate(threads, true) == threads.length) {
      threads = new Thread[threads.length * 2];
    }

    // threads
    for (Thread t : threads) {
      // Only display the threads part of the current group
      if (t != null) {
        xml.openElement("thread", true);
        xml.attribute("id", Long.toString(t.getId()));
        xml.attribute("name", t.getName());
        xml.attribute("priority", t.getPriority());
        xml.attribute("state", t.getState().name());
        xml.attribute("alive", Boolean.toString(t.isAlive()));
        xml.attribute("daemon", Boolean.toString(t.isDaemon()));
        xml.attribute("group", t.getThreadGroup().getName());
        if (t == Thread.currentThread()) {
          // Flag the current thread
          xml.attribute("current", "true");
        }

        if (bean != null) {
          final long _cpu = bean.getThreadCpuTime(t.getId());
          final long _user = bean.getThreadUserTime(t.getId());
          xml.openElement("times");
          xml.attribute("cpu", Long.toString(_cpu));
          xml.attribute("user", Long.toString(_user));
          xml.attribute("system", Long.toString(_cpu - _user));
          xml.closeElement();
        }

        xml.closeElement();
      }
    }
  }

  /**
   * Return all the threads with stack traces
   *
   * @param thread     The thread to serialise as XML
   * @param stacktrace The stack trace (may be <code>null</code>)
   * @param bean       The management bean for CPU time (may be <code>null</code>)
   * @param xml The XML writer
   *
   * @throws IOException If thrown while writing XML.
   */
  private static void toXML(Thread thread, StackTraceElement[] stacktrace, ThreadMXBean bean, XMLWriter xml)
      throws IOException {
    xml.openElement("thread", true);
    xml.attribute("id", Long.toString(thread.getId()));
    xml.attribute("name", thread.getName());
    xml.attribute("priority", thread.getPriority());
    xml.attribute("state", thread.getState().name());
    xml.attribute("alive", Boolean.toString(thread.isAlive()));
    xml.attribute("daemon", Boolean.toString(thread.isDaemon()));
    xml.attribute("group", thread.getThreadGroup().getName());
    if (thread == Thread.currentThread()) {
      // Flag the current thread
      xml.attribute("current", "true");
    }

    // If the management bean is available include times
    if (bean != null) {
      final long cpu = bean.getThreadCpuTime(thread.getId());
      final long user = bean.getThreadUserTime(thread.getId());
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

}
