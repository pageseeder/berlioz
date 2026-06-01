package org.pageseeder.berlioz.http;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

class PerformanceServerTimingTest {

  @Test
  void testContructor_NullName() {
    Assertions.assertThrows(NullPointerException.class, () -> new PerformanceServerTiming(null, 0));
  }

  @Test
  void testContructor_EmptyName() {
    Assertions.assertThrows(IllegalArgumentException.class, () -> new PerformanceServerTiming("", 0));
  }

  @Test
  void testContructor_InvalidName() {
    // Illegal names are (),/:;<=>?@[\]{} and "
    Assertions.assertThrows(IllegalArgumentException.class, () -> new PerformanceServerTiming("{", 0));
  }

  @Test
  void testContructor_ValidName() {
    PerformanceServerTiming a =new PerformanceServerTiming("a.b!C$D", 0);
    Assertions.assertEquals(a.name(), "a.b!C$D");
    Assertions.assertEquals(0, a.duration(), 0);
  }

  @Test
  void testContructor_EmptyDescription() {
    PerformanceServerTiming a = new PerformanceServerTiming("x", null, 1.2);
    PerformanceServerTiming b = new PerformanceServerTiming("x", "", 1.2);
    PerformanceServerTiming c = new PerformanceServerTiming("x", 1.2);
    Assertions.assertEquals(a.description(), "");
    Assertions.assertEquals(b.description(), "");
    Assertions.assertEquals(c.description(), "");
  }

  @Test
  void testContructor_ValidDescription() {
    PerformanceServerTiming t = new PerformanceServerTiming("x", "Test", 1.2);
    Assertions.assertEquals(t.description(), "Test");
  }

  @Test
  void testContructor_InvalidDescription1() {
    PerformanceServerTiming t = new PerformanceServerTiming("x", " ", 1.2);
    Assertions.assertEquals(t.description(), "_");
  }

  @Test
  void testContructor_InvalidDescription2() {
    PerformanceServerTiming t = new PerformanceServerTiming("x", "A B\nC", 1.2);
    Assertions.assertEquals(t.description(), "A__B_C");
  }

  @Test
  void testContructor_InvalidDescription3() {
    PerformanceServerTiming t = new PerformanceServerTiming("x", "A\n\rB", 1.2);
    Assertions.assertEquals(t.description(), "A__B");
  }

  @Test
  void testToHeader1() {
    PerformanceServerTiming timing = new PerformanceServerTiming("abc", "xyz", 1.2);
    Assertions.assertEquals(timing.toHeaderString(), "abc;desc=xyz;dur=1.2");
  }

  @Test
  void testToHeader2() {
    PerformanceServerTiming timing = new PerformanceServerTiming("abc", 1.2);
    Assertions.assertEquals(timing.toHeaderString(), "abc;dur=1.2");
  }

  @Test
  void testToHeader3() {
    PerformanceServerTiming timing = new PerformanceServerTiming("abc", "Requires quotes",1.2);
    Assertions.assertEquals(timing.toHeaderString(), "abc;desc=\"Requires quotes\";dur=1.2");
  }

  @Test
  void testToHeader4() {
    PerformanceServerTiming timing = new PerformanceServerTiming("abc", "()",1.2);
    Assertions.assertEquals(timing.toHeaderString(), "abc;desc=\"()\";dur=1.2");
  }

  @Test
  void testToHeader5() {
    PerformanceServerTiming timing = new PerformanceServerTiming("abc", "\"a\\b\"",1.2);
    Assertions.assertEquals(timing.toHeaderString(), "abc;desc=\"\\\"a\\\\b\\\"\";dur=1.2");
  }

  @Test
  void testToHeader6() {
    PerformanceServerTiming timing = new PerformanceServerTiming("abc",Math.PI);
    Assertions.assertEquals(timing.toHeaderString(), "abc;dur=3.142");
  }

  @Test
  void testToHeader7() {
    PerformanceServerTiming timing = new PerformanceServerTiming("abc", 2);
    Assertions.assertEquals(timing.toHeaderString(), "abc;dur=2");
  }

  @Test
  void testToHeader8() {
    PerformanceServerTiming timing = new PerformanceServerTiming("abc", -1);
    Assertions.assertEquals(timing.toHeaderString(), "abc");
  }

  @Test
  void testToHeader9() {
    PerformanceServerTiming timing = new PerformanceServerTiming("abc", 0);
    Assertions.assertEquals(timing.toHeaderString(), "abc;dur=0");
  }

  @Test
  void testToHeader10() {
    PerformanceServerTiming timing = new PerformanceServerTiming("abc", 0.0005);
    Assertions.assertEquals(timing.toHeaderString(), "abc;dur=0.001");
  }

}
