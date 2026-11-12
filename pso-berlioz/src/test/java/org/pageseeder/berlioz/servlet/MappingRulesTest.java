package org.pageseeder.berlioz.servlet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

/**
 * Concurrent loading of the rules of a filter.
 */
class MappingRulesTest {

  private static final int THREADS = 8;

  @TempDir File folder;

  private final AtomicInteger loads = new AtomicInteger();
  private final CountDownLatch loading = new CountDownLatch(1);
  private final CountDownLatch release = new CountDownLatch(1);
  private ExecutorService executor;

  @BeforeEach
  void setup() {
    this.executor = Executors.newFixedThreadPool(THREADS);
  }

  @AfterEach
  void cleanup() {
    this.release.countDown();
    this.executor.shutdownNow();
  }

  @Test
  void concurrentFirstUseLoadsOnce() throws Exception {
    MappingRules<Object> rules = rules();
    List<Future<Object>> results = new ArrayList<>();
    results.add(this.executor.submit(rules::get));
    assertTrue(this.loading.await(5, TimeUnit.SECONDS), "The first request should start loading");
    for (int i = 1; i < THREADS; i++) results.add(this.executor.submit(rules::get));
    // Let the other requests reach the lock while the first one loads
    Thread.sleep(100);
    this.release.countDown();

    Object first = get(results.get(0));
    for (Future<Object> result : results) assertSame(first, get(result));
    assertEquals(1, this.loads.get());
  }

  @Test
  void reloadDuringLoadLoadsOnceMore() throws Exception {
    MappingRules<Object> rules = rules();
    Future<Object> before = this.executor.submit(rules::get);
    assertTrue(this.loading.await(5, TimeUnit.SECONDS), "The first request should start loading");
    MappingFilters.requestReload(ServletTestSupport.request().uri("/a").build());
    // Waits for the first load, then loads the rules of the new generation
    Future<Object> after = this.executor.submit(rules::get);
    Thread.sleep(100);
    this.release.countDown();

    Object old = get(before);
    Object current = get(after);
    assertEquals(2, this.loads.get());
    // The rules of the new generation are kept: no further load
    assertSame(current, rules.get());
    assertEquals(2, this.loads.get());
    assertTrue(old != current, "The new generation should be loaded again");
  }

  private MappingRules<Object> rules() {
    return new MappingRules<>("redirect", "test", "config.xml", new File(this.folder, "config.xml"),
        LoggerFactory.getLogger(MappingRulesTest.class), this::load, new Object(),
        config -> List.of(), config -> List.of());
  }

  /** Blocks the first load until released, so that other requests arrive while it loads. */
  private Object load(File file) {
    if (this.loads.incrementAndGet() == 1) {
      this.loading.countDown();
      try {
        this.release.await(5, TimeUnit.SECONDS);
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
      }
    }
    return new Object();
  }

  private static Object get(Future<Object> result) throws InterruptedException, ExecutionException, TimeoutException {
    return result.get(5, TimeUnit.SECONDS);
  }

}
