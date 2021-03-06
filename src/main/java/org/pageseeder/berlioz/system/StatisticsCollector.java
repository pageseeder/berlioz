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
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicLong;

import org.pageseeder.berlioz.content.ContentGenerator;
import org.pageseeder.berlioz.content.ContentStatus;
import org.pageseeder.berlioz.content.GeneratorListener;
import org.pageseeder.berlioz.content.Service;
import org.pageseeder.berlioz.util.ISO8601;
import org.pageseeder.xmlwriter.XMLWritable;
import org.pageseeder.xmlwriter.XMLWriter;

/**
 * Collects basic statistics about generators.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.11.2
 * @since Berlioz 0.9.32
 */
final class StatisticsCollector implements GeneratorListener, XMLWritable {

  /**
   * Singleton instance.
   */
  private static final StatisticsCollector SINGLETON = new StatisticsCollector();

  /**
   * All the statistics.
   */
  private final ConcurrentHashMap<Class<?>, BasicStats> _stats = new ConcurrentHashMap<>();

  /**
   * When did we start collecting statistics
   */
  private long since = System.currentTimeMillis();

  /**
   * Use <code>getInstance</code> instead.
   */
  private StatisticsCollector() {
  }

  @Override
  public void generate(Service service, ContentGenerator generator, ContentStatus status, long etag, long process) {
    BasicStats basic = this._stats.get(generator.getClass());

    // Create entry if it does not exist
    if (basic == null) {
      basic = new BasicStats(generator.getClass().getName(), status, etag, process);
      this._stats.put(generator.getClass(), basic);
    } else {
      // update
      basic.update(status, etag, process);
    }
  }

  /**
   * Clears all the statistics.
   */
  public void clear() {
    this._stats.clear();
    synchronized (this) {
      this.since = System.currentTimeMillis();
    }
  }

  /**
   * @return The singleton instance.
   */
  public static StatisticsCollector getInstance() {
    return SINGLETON;
  }

  @Override
  public void toXML(XMLWriter xml) throws IOException {
    xml.openElement("statistics");
    xml.attribute("since", ISO8601.format(this.since, ISO8601.DATETIME));
    for (BasicStats s : this._stats.values()) {
      s.toXML(xml);
    }
    xml.closeElement();
  }

  /**
   * Holds basic statistics about a generator.
   */
  public static final class BasicStats implements XMLWritable {

    /**
     * The class name of the generator.
     */
    private final String _generator;

    /**
     * The number of times the method was invoked.
     */
    private final Map<ContentStatus, AtomicLong> _status;

    /**
     * The number of times the method was invoked.
     */
    private final AtomicLong _count;

    /**
     * Total amount of time taken by the getEtag() method in microseconds.
     */
    private final AtomicLong _totalEtagTime;

    /**
     * Total amount of time taken by the process() method in microseconds.
     */
    private final AtomicLong _totalProcessTime;

    /**
     * Minimum time taken by the getEtag() method in microseconds.
     */
    private final AtomicLong _minEtagTime;

    /**
     * Minimum time taken by the process() method in microseconds.
     */
    private final AtomicLong _minProcessTime;

    /**
     * Maximum time taken by the getEtag() method in microseconds.
     */
    private final AtomicLong _maxEtagTime;

    /**
     * Maximum time taken by the process() method in microseconds.
     */
    private final AtomicLong _maxProcessTime;

    /**
     * Maximum time taken by the process() method in microseconds.
     */
    private final LinkedBlockingDeque<Long> _lastEtag = new LinkedBlockingDeque<>(10);

    /**
     * Maximum time taken by the process() method in microseconds.
     */
    private final LinkedBlockingDeque<Long> _lastProcess = new LinkedBlockingDeque<>(10);

    /**
     * Creates a instance with the specified initial status and time values.
     *
     * @param name    The name of the generator
     * @param status  The first content status
     * @param etag    The etag time in nano seconds
     * @param process The process time in nano seconds
     */
    private BasicStats(String name, ContentStatus status, long etag, long process) {
      this._generator = name;
      this._status = Collections.synchronizedMap(new EnumMap<>(ContentStatus.class));
      this._status.put(status, new AtomicLong(1));
      this._count = new AtomicLong(1);
      long e = etag / 1000;
      this._minEtagTime = new AtomicLong(e);
      this._maxEtagTime = new AtomicLong(e);
      this._totalEtagTime = new AtomicLong(e);
      long p = process / 1000;
      this._minProcessTime = new AtomicLong(p);
      this._maxProcessTime = new AtomicLong(p);
      this._totalProcessTime = new AtomicLong(p);
    }

    /**
     * Update the statistics.
     *
     * @param status  The content status
     * @param etag    The getEtag() function time in nano seconds
     * @param process The process() function time in nano seconds
     */
    public synchronized void update(ContentStatus status, long etag, long process) {
      // Status
      AtomicLong i = this._status.get(status);
      if (i == null) {
        this._status.put(status, new AtomicLong(1));
      } else {
        i.incrementAndGet();
      }

      this._count.incrementAndGet();
      // times in microseconds
      long e = etag / 1000;
      long p = process / 1000;
      // min and max
      if (e > this._maxEtagTime.get()) {
        this._maxEtagTime.set(e);
      }
      if (e < this._minEtagTime.get()) {
        this._minEtagTime.set(e);
      }
      if (p > this._maxProcessTime.get()) {
        this._maxProcessTime.set(p);
      }
      if (p < this._minProcessTime.get()) {
        this._minProcessTime.set(p);
      }
      // Total
      this._totalEtagTime.addAndGet(e);
      this._totalProcessTime.addAndGet(p);
      if (this._lastEtag.remainingCapacity() == 0) {
        this._lastEtag.pollFirst();
      }
      this._lastEtag.offerLast(e);
      if (this._lastProcess.remainingCapacity() == 0) {
        this._lastProcess.pollFirst();
      }
      this._lastProcess.offerLast(p);
    }

    @Override
    public synchronized void toXML(XMLWriter xml) throws IOException {
      xml.openElement("statistic");
      xml.attribute("generator", this._generator);
      xml.attribute("count", this._count.toString());
      // times
      xml.attribute("min-etag",      this._minEtagTime.toString());
      xml.attribute("min-process",   this._minProcessTime.toString());
      xml.attribute("max-etag",      this._maxEtagTime.toString());
      xml.attribute("max-process",   this._maxProcessTime.toString());
      xml.attribute("total-etag",    this._totalEtagTime.toString());
      xml.attribute("total-process", this._totalProcessTime.toString());
      // compute the average
      long avgEtag = this._totalEtagTime.longValue() / this._count.longValue();
      long avgProcess = this._totalProcessTime.longValue() / this._count.longValue();
      xml.attribute("avg-etag",    Long.toString(avgEtag));
      xml.attribute("avg-process", Long.toString(avgProcess));

      long avgLastEtag = average(this._lastEtag);
      long avgLastProcess = average(this._lastProcess);
      xml.attribute("avg-last-etag",    Long.toString(avgLastEtag));
      xml.attribute("avg-last-process", Long.toString(avgLastProcess));

      // status
      xml.openElement("status");
      for (Entry<ContentStatus, AtomicLong> status : this._status.entrySet()) {
        xml.attribute(status.getKey().name().toLowerCase(), Long.toString(status.getValue().get()));
      }
      xml.closeElement();
      xml.closeElement();
    }

    /**
     * @param times Times to average
     * @return The average time value
     */
    private static long average(Collection<Long> times) {
      if (times.isEmpty()) return 0;
      long avgLastEtag = 0;
      for (Long t : times) {
        avgLastEtag += t;
      }
      return avgLastEtag / times.size();
    }
  }
}
