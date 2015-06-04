/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.system;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

import org.weborganic.berlioz.BerliozException;
import org.weborganic.berlioz.content.ContentGenerator;
import org.weborganic.berlioz.content.ContentRequest;
import org.weborganic.berlioz.content.ContentStatus;

import com.topologi.diffx.xml.XMLWriter;

/**
 * Returns the User, System and CPU times.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.9.32
 * @since Berlioz 0.9.32
 */
public final class GetCPUTime implements ContentGenerator {

  @Override
  public void process(ContentRequest req, XMLWriter xml) throws BerliozException, IOException {

    int interval = req.getIntParameter("interval", 100);

    // Check that the interval is positive
    if (interval <= 0) {
      req.setStatus(ContentStatus.BAD_REQUEST);
      xml.writeComment("Interval must be strictly positive");
      return;
    }

    long threadId = -1L;
    try {
      threadId = Long.parseLong(req.getParameter("thread", "-1"));
    } catch (NumberFormatException ex) {
      req.setStatus(ContentStatus.BAD_REQUEST);
      xml.writeComment("Invalid thread ID");
      return;
    }

    try {
      ThreadMXBean bean = ManagementFactory.getThreadMXBean();
      // measure
      Sample start;
      Sample end;
      if (threadId == -1L) {
        long current = Thread.currentThread().getId();
        start = global(bean, current);
        Thread.sleep(interval);
        end = global(bean, current);
      } else {
        start = single(bean, threadId);
        Thread.sleep(interval);
        end = single(bean, threadId);
      }

      // Calculate
      long time = end.time() - start.time();
      long user = end.user() - start.user();
      long cpu = end.cpu() - start.cpu();

      // Write XML
      xml.openElement("sample");
      xml.attribute("interval", Long.toString(interval));
      xml.attribute("cpu", Long.toString(cpu*100 / time));
      xml.attribute("user", Long.toString(user*100 / time));
      xml.attribute("system", Long.toString((cpu - user)*100 / time));
      xml.closeElement();

    } catch (InterruptedException ex) {

    }
  }

  /**
   * Return a sample for the whole system.
   *
   * @param bean The thread management instance
   * @param current the ID of the current thread.
   *
   * @return the corresponding sample
   */
  private Sample global(ThreadMXBean bean, long current) {
    long cpu = 0L;
    long user = 0L;
    final long[] _ids = bean.getAllThreadIds();
    for (long id : _ids) {
      // Exclude this thread
      if (id == current) {
        continue;
      }
      final long _c = bean.getThreadCpuTime(id);
      final long _u = bean.getThreadUserTime(id);
      // Ignore dead threads
      if (_c == -1 || _u == -1) {
        continue;
      }
      cpu += _c;
      user += _u;
    }
    return new Sample(cpu, user);
  }

  /**
   * Return a sample for a single thread
   *
   * @param bean The thread management instance
   * @param id   The ID of the thread to measure.
   *
   * @return the corresponding sample
   */
  private Sample single(ThreadMXBean bean, long id) {
    final long _cpu = bean.getThreadCpuTime(id);
    final long _user = bean.getThreadUserTime(id);
    // The thread has died!
    if (_cpu == -1 || _user == -1) return new Sample(0L, 0L);
    else return new Sample(_cpu, _user);
  }

  /**
   * Co
   */
  private static class Sample {
    public final long _time = System.nanoTime();
    public final long _cpu;
    public final long _user;
    public Sample(long cpu, long user) {
      this._cpu = cpu;
      this._user = user;
    }
    public long cpu() {
      return this._cpu;
    }
    public long user() {
      return this._user;
    }
    public long time() {
      return this._time;
    }
  }

}
