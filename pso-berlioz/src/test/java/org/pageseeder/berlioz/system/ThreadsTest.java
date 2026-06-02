package org.pageseeder.berlioz.system;

import org.junit.jupiter.api.Test;

import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

import static org.junit.jupiter.api.Assertions.*;

class ThreadsTest {

  @Test
  void testGetThreadMXBean_nonNull() {
    assertNotNull(Threads.getThreadMXBean());
  }

  @Test
  void testGetThreadInfo_allThreads_nonEmpty() {
    ThreadMXBean bean = Threads.getThreadMXBean();
    ThreadInfo[] infos = Threads.getThreadInfo(bean, 0);
    assertNotNull(infos);
    assertTrue(infos.length > 0, "Should have at least the current thread");
  }

  @Test
  void testGetThreadInfo_currentThread_nonNull() {
    long id = Thread.currentThread().getId();
    ThreadInfo info = Threads.getThreadInfo(id);
    assertNotNull(info);
    assertEquals(id, info.getThreadId());
  }

  @Test
  void testGetThreadInfo_nonExistentThread_returnsNull() {
    // Thread ID Long.MAX_VALUE is extremely unlikely to exist
    ThreadInfo info = Threads.getThreadInfo(Long.MAX_VALUE);
    assertNull(info);
  }

  @Test
  void testThreadGroupName_isPlaceholder() {
    String name = Threads.threadGroupName();
    assertNotNull(name);
    assertFalse(name.isEmpty());
  }
}
