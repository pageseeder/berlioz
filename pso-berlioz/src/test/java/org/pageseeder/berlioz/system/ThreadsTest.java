package org.pageseeder.berlioz.system;

import org.junit.jupiter.api.Test;

import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BooleanSupplier;

import org.pageseeder.berlioz.output.XmlOutputAdapter;

import static org.junit.jupiter.api.Assertions.*;

class ThreadsTest {

  @Test
  void testGetThreadMXBean_nonNull() {
    assertNotNull(Threads.getThreadMXBean());
  }

  @Test
  void testGetThreadInfo_allThreads_nonEmpty() {
    ThreadMXBean bean = Threads.getThreadMXBean();
    ThreadInfo[] infos = Threads.getThreadInfo(bean, bean.getAllThreadIds(), false);
    assertNotNull(infos);
    assertTrue(infos.length > 0, "Should have at least the current thread");
  }

  @Test
  void testGetThreadInfo_currentThread_nonNull() {
    long id = Thread.currentThread().getId();
    ThreadInfo info = Threads.getThreadInfo(Threads.getThreadMXBean(), id);
    assertNotNull(info);
    assertEquals(id, info.getThreadId());
  }

  @Test
  void testGetThreadInfo_nonExistentThread_returnsNull() {
    // Thread ID Long.MAX_VALUE is extremely unlikely to exist
    ThreadInfo info = Threads.getThreadInfo(Threads.getThreadMXBean(), Long.MAX_VALUE);
    assertNull(info);
  }

  @Test
  void testThreadGroupName_isPlaceholder() {
    String name = Threads.threadGroupName();
    assertNotNull(name);
    assertFalse(name.isEmpty());
  }

  @Test
  void testLimit_belowLimit_returnsSameIds() {
    long[] ids = {1, 2, 3};
    assertSame(ids, Threads.limit(ids));
  }

  @Test
  void testLimit_aboveLimit_truncates() {
    long[] ids = new long[Threads.MAX_THREADS + 1];
    for (int i = 0; i < ids.length; i++) ids[i] = i;
    long[] limited = Threads.limit(ids);
    assertEquals(Threads.MAX_THREADS, limited.length);
    assertEquals(Threads.MAX_THREADS - 1, limited[limited.length - 1]);
  }

  @Test
  void testGetThreadInfo_stackTraces_readsOneFrameBeyondLimit() {
    ThreadMXBean bean = Threads.getThreadMXBean();
    long id = Thread.currentThread().getId();
    ThreadInfo info = recurse(Threads.MAX_STACK_DEPTH + 10,
        () -> Threads.getThreadInfo(bean, new long[]{id}, true)[0]);
    assertEquals(Threads.MAX_STACK_DEPTH + 1, info.getStackTrace().length);
    assertTrue(Threads.isStackTruncated(info));
  }

  @Test
  void testGetThreadInfo_noStackTraces_emptyStack() {
    ThreadMXBean bean = Threads.getThreadMXBean();
    ThreadInfo info = Threads.getThreadInfo(bean, new long[]{Thread.currentThread().getId()}, false)[0];
    assertEquals(0, info.getStackTrace().length);
    assertFalse(Threads.isStackTruncated(info));
  }

  @Test
  void testWriteStackTrace_truncatesToLimit() {
    StackTraceElement[] stack = new StackTraceElement[Threads.MAX_STACK_DEPTH + 50];
    for (int i = 0; i < stack.length; i++) stack[i] = new StackTraceElement("C", "m" + i, "C.java", i);
    XmlOutputAdapter out = new XmlOutputAdapter();
    Threads.writeStackTrace(out, stack);
    String xml = out.toString();
    assertEquals(Threads.MAX_STACK_DEPTH, xml.split("<element ", -1).length - 1);
    assertTrue(xml.contains("method=\"m255\""), xml);
    assertFalse(xml.contains("method=\"m256\""), xml);
  }

  @Test
  void testGetAllocatedBytes_currentThread() {
    long[] bytes = Threads.getAllocatedBytes(new long[]{Thread.currentThread().getId()});
    // Provided by HotSpot-based JVMs; absent elsewhere
    if (bytes != null) {
      assertEquals(1, bytes.length);
      assertTrue(bytes[0] > 0, "Current thread should have allocated memory");
    }
  }

  @Test
  void testGetAllocatedBytes_unknownThread_notAvailable() {
    long[] bytes = Threads.getAllocatedBytes(new long[]{Long.MAX_VALUE});
    if (bytes != null) assertTrue(bytes[0] < 0);
  }

  @Test
  void testGetDeadlockedThreads_noDeadlock_empty() {
    assertTrue(Threads.getDeadlockedThreads(Threads.getThreadMXBean()).isEmpty());
  }

  @Test
  void testGetDeadlockedThreads_detectsDeadlock() throws Exception {
    try (Deadlock deadlock = new Deadlock()) {
      assertTrue(Threads.getDeadlockedThreads(Threads.getThreadMXBean())
          .containsAll(java.util.List.of(deadlock.first.getId(), deadlock.second.getId())));
    }
  }

  /**
   * Calls the supplier at the specified extra stack depth.
   */
  static <T> T recurse(int depth, java.util.function.Supplier<T> supplier) {
    return depth == 0 ? supplier.get() : recurse(depth - 1, supplier);
  }

  /**
   * Polls the condition until it holds or 5 seconds elapse.
   *
   * <p>Thread states and deadlocks are only observable through polling, there is no event to wait on.
   *
   * @return whether the condition was met before the timeout
   */
  static boolean awaitCondition(BooleanSupplier condition) {
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
    while (!condition.getAsBoolean()) {
      if (System.nanoTime() >= deadline) return false;
      LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(10));
    }
    return true;
  }

  /**
   * Two daemon threads deadlocked on reentrant locks, interrupted on close.
   */
  static final class Deadlock implements AutoCloseable {

    final Thread first;
    final Thread second;

    Deadlock() throws InterruptedException {
      ReentrantLock a = new ReentrantLock();
      ReentrantLock b = new ReentrantLock();
      CountDownLatch locked = new CountDownLatch(2);
      this.first = start("deadlock-1", a, b, locked);
      this.second = start("deadlock-2", b, a, locked);
      assertTrue(locked.await(5, TimeUnit.SECONDS));
      ThreadMXBean bean = Threads.getThreadMXBean();
      assertTrue(awaitCondition(() -> Threads.getDeadlockedThreads(bean).size() >= 2));
    }

    private static Thread start(String name, ReentrantLock held, ReentrantLock wanted, CountDownLatch locked) {
      Thread thread = new Thread(() -> {
        held.lock();
        try {
          locked.countDown();
          // Both threads hold their first lock before either asks for the second
          locked.await();
          wanted.lockInterruptibly();
          wanted.unlock();
        } catch (InterruptedException ex) {
          Thread.currentThread().interrupt();
        } finally {
          held.unlock();
        }
      }, name);
      thread.setDaemon(true);
      thread.start();
      return thread;
    }

    @Override
    public void close() throws InterruptedException {
      this.first.interrupt();
      this.second.interrupt();
      this.first.join(5000);
      this.second.join(5000);
    }
  }
}
