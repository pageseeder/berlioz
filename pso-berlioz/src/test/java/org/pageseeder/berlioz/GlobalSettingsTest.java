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
package org.pageseeder.berlioz;

import java.io.File;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test class for the global settings
 *
 * @author Christophe Lauret
 *
 * @version 0.10.3
 * @since 0.10.3
 */
@SuppressWarnings({"deprecated", "java:S1874"})
final class GlobalSettingsTest {

  @BeforeEach
  void setup() {
    File webinf = new File("src/test/resources/org/pageseeder/berlioz");
    System.out.println(webinf.getAbsolutePath());
    InitEnvironment env = InitEnvironment.create(webinf).mode("default");
    GlobalSettings.setup(env);
  }

  /**
   * <string sample="hello" empty=""/>
   */
  @Test
  void testGet_String() {
    // Value is "hello" -> "hello"
    Assertions.assertEquals("hello", GlobalSettings.get("test.string.sample"));
    Assertions.assertEquals("hello", GlobalSettings.get("test.string.sample", "byebye"));
    // Value is empty ("") -> ""
    Assertions.assertEquals("", GlobalSettings.get("test.string.empty"));
    Assertions.assertEquals("", GlobalSettings.get("test.string.empty", "fallback-1"));
    // Value is undefined (null) -> default
    Assertions.assertNull(GlobalSettings.get("test.string.undefined"));
    Assertions.assertEquals("fallback-2", GlobalSettings.get("test.string.undefined", "fallback-2"));
  }

  @Test
  void testGet_Boolean_TrueAndFalse() {
    Assertions.assertEquals("true", GlobalSettings.get("test.boolean.true"));
    Assertions.assertTrue(GlobalSettings.get("test.boolean.true", true));
    Assertions.assertTrue(GlobalSettings.get("test.boolean.true", false));
    Assertions.assertEquals("false", GlobalSettings.get("test.boolean.false"));
    Assertions.assertFalse(GlobalSettings.get("test.boolean.false", true));
    Assertions.assertFalse(GlobalSettings.get("test.boolean.false", false));
  }

  @Test
  void testGet_Boolean_Invalid() {
    Assertions.assertTrue(GlobalSettings.get("test.boolean.invalid", true));
    Assertions.assertFalse(GlobalSettings.get("test.boolean.invalid", false));
    Assertions.assertEquals("True", GlobalSettings.get("test.boolean.invalid-true"));
    Assertions.assertTrue(GlobalSettings.get("test.boolean.invalid-true", true));
    Assertions.assertFalse(GlobalSettings.get("test.boolean.invalid-true", false));
    Assertions.assertEquals("FALSE", GlobalSettings.get("test.boolean.invalid-false"));
    Assertions.assertTrue(GlobalSettings.get("test.boolean.invalid-false", true));
    Assertions.assertFalse(GlobalSettings.get("test.boolean.invalid-false", false));
    Assertions.assertEquals("yes", GlobalSettings.get("test.boolean.invalid-yes"));
    Assertions.assertTrue(GlobalSettings.get("test.boolean.invalid-yes", true));
    Assertions.assertFalse(GlobalSettings.get("test.boolean.invalid-yes", false));
    Assertions.assertEquals("no", GlobalSettings.get("test.boolean.invalid-no"));
    Assertions.assertTrue(GlobalSettings.get("test.boolean.invalid-no", true));
    Assertions.assertFalse(GlobalSettings.get("test.boolean.invalid-no", false));
  }

  @Test
  void testGet_Boolean_EmptyAndUndefined() {
    Assertions.assertEquals("", GlobalSettings.get("test.boolean.empty"));
    Assertions.assertTrue(GlobalSettings.get("test.boolean.empty", true));
    Assertions.assertFalse(GlobalSettings.get("test.boolean.empty", false));
    Assertions.assertNull(GlobalSettings.get("test.boolean.undefined"));
    Assertions.assertTrue(GlobalSettings.get("test.boolean.undefined", true));
    Assertions.assertFalse(GlobalSettings.get("test.boolean.undefined", false));
  }

  /**
   * <int valid="123" toolarge="129999999999999999999999" invalid="not_an_int" empty=""/>
   */
  @Test
  void testGet_Int() {
    // Value is a number
    Assertions.assertEquals("123", GlobalSettings.get("test.int.valid"));
    Assertions.assertEquals(123, GlobalSettings.get("test.int.valid", 777));
    // Value is larger than MAX_INTEGER
    Assertions.assertEquals(777, GlobalSettings.get("test.int.toolarge", 777));
    // Value is not an integer
    Assertions.assertEquals(777, GlobalSettings.get("test.int.invalid", 777));
    // Value is empty
    Assertions.assertEquals("", GlobalSettings.get("test.int.empty"));
    Assertions.assertEquals(777, GlobalSettings.get("test.int.empty", 777));
    Assertions.assertEquals(777, GlobalSettings.get("test.int.empty", 777));
    // Value is undefined (null)
    Assertions.assertNull(GlobalSettings.get("test.int.undefined"));
    Assertions.assertEquals(777, GlobalSettings.get("test.int.undefined", 777));
    Assertions.assertEquals(777, GlobalSettings.get("test.int.undefined", 777));
  }

  @Test
  void testSetMode() {
    Assertions.assertEquals("default", GlobalSettings.getMode());
    GlobalSettings.setMode("test");
    Assertions.assertEquals("test", GlobalSettings.getMode());
    GlobalSettings.setMode("default");
    Assertions.assertEquals("default", GlobalSettings.getMode());
  }

  @Test
  void testSetMode_Null() {
    Assertions.assertThrows(NullPointerException.class, () -> GlobalSettings.setMode(null));
  }

  @Test
  void testLoad_Override() {
    GlobalSettings.setMode("undefined"); // Loads without the override
    GlobalSettings.load();
    Assertions.assertEquals("true", GlobalSettings.get("berlioz.xslt.cache"));
    Assertions.assertEquals("default", GlobalSettings.get("app.location"));
    Assertions.assertEquals("true", GlobalSettings.get("app.cache"));
    Assertions.assertNull(GlobalSettings.get("app.name"));

    GlobalSettings.setMode("override1"); // Loads with the override (xml)
    GlobalSettings.load();
    Assertions.assertEquals("false", GlobalSettings.get("berlioz.xslt.cache"));
    Assertions.assertEquals("app1", GlobalSettings.get("app.name"));
    Assertions.assertEquals("default", GlobalSettings.get("app.location"));
    Assertions.assertEquals("false", GlobalSettings.get("app.cache"));

    GlobalSettings.setMode("override2"); // Loads with the override (properties)
    GlobalSettings.load();
    Assertions.assertEquals("false", GlobalSettings.get("berlioz.xslt.cache"));
    Assertions.assertEquals("app2", GlobalSettings.get("app.name"));
    Assertions.assertEquals("default", GlobalSettings.get("app.location"));
    Assertions.assertEquals("false", GlobalSettings.get("app.cache"));

  }

  @Test
  void testLoad_Errors() {
    Assertions.assertTrue(GlobalSettings.load());
    Assertions.assertNotEquals(0, GlobalSettings.countProperties());

    GlobalSettings.setMode("empty");
    Assertions.assertFalse(GlobalSettings.load());
    Assertions.assertEquals(0, GlobalSettings.countProperties());

    GlobalSettings.setMode("invalid");
    Assertions.assertFalse(GlobalSettings.load());
    Assertions.assertEquals(0, GlobalSettings.countProperties());
  }

  @Test
  void testLoad_Listeners() {
    final class NotifiableConfigListener implements ConfigListener {
      public int notifications = 0;
      @Override public void load() {
        this.notifications++;
      }
    }

    // Listener that behaves properly
    final NotifiableConfigListener good = new NotifiableConfigListener();

    // Listener that throws an exception
    final ConfigListener bad = new ConfigListener() {
      final RuntimeException ignore = new RuntimeException("You can ignore this exception.");
      @Override public void load() {
        this.ignore.setStackTrace(new StackTraceElement[]{});
        throw this.ignore;
      }
    };

    // Register good listener and check that it received notification
    GlobalSettings.registerListener(good);
    Assertions.assertTrue(GlobalSettings.load());
    Assertions.assertEquals(1, good.notifications);

    // Register bad listener and check that the good listener received notification and no exception is thrown
    GlobalSettings.registerListener(bad);
    Assertions.assertTrue(GlobalSettings.load());
    Assertions.assertEquals(2, good.notifications);

    // Check that listener is not notified if load fails
    GlobalSettings.setMode("empty");
    Assertions.assertFalse(GlobalSettings.load());
    Assertions.assertEquals(2,  good.notifications);
    GlobalSettings.removeAllListeners();
  }
}
