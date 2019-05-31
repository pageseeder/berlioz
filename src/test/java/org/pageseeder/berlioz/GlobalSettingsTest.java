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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test class for the global settings
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.10.3
 * @since Berlioz 0.10.3
 */
public final class GlobalSettingsTest {

  @Before
  public void setup() {
    File webinf = new File("src/test/resources/org/pageseeder/berlioz");
    System.out.println(webinf.getAbsolutePath());
    InitEnvironment env = InitEnvironment.create(webinf).mode("default");
    GlobalSettings.setup(env);
  }

  /**
   * <string sample="hello" empty=""/>
   */
  @Test
  public void testGet_String() {
    // Value is "hello" -> "hello"
    Assert.assertEquals("hello", GlobalSettings.get("test.string.sample"));
    Assert.assertEquals("hello", GlobalSettings.get("test.string.sample", "byebye"));
    // Value is empty ("") -> ""
    Assert.assertEquals("", GlobalSettings.get("test.string.empty"));
    Assert.assertEquals("", GlobalSettings.get("test.string.empty", "fallback-1"));
    // Value is undefined (null) -> default
    Assert.assertNull(GlobalSettings.get("test.string.undefined"));
    Assert.assertEquals("fallback-2", GlobalSettings.get("test.string.undefined", "fallback-2"));
  }

  /**
   * <boolean true="true" false="false" invalid="coconut" empty=""/>
   */
  @Test
  public void testGet_Boolean() {
    // Value is 'true' -> true
    Assert.assertEquals("true", GlobalSettings.get("test.boolean.true"));
    Assert.assertTrue(GlobalSettings.get("test.boolean.true", true));
    Assert.assertTrue(GlobalSettings.get("test.boolean.true", false));
    // Value is 'false' -> false
    Assert.assertEquals("false", GlobalSettings.get("test.boolean.false"));
    Assert.assertFalse(GlobalSettings.get("test.boolean.false", true));
    Assert.assertFalse(GlobalSettings.get("test.boolean.false", false));
    // Value is not a valid boolean -> default
    Assert.assertTrue(GlobalSettings.get("test.boolean.invalid", true));
    Assert.assertFalse(GlobalSettings.get("test.boolean.invalid", false));
    Assert.assertEquals("True", GlobalSettings.get("test.boolean.invalid-true"));
    Assert.assertTrue(GlobalSettings.get("test.boolean.invalid-true", true));
    Assert.assertFalse(GlobalSettings.get("test.boolean.invalid-true", false));
    Assert.assertEquals("FALSE", GlobalSettings.get("test.boolean.invalid-false"));
    Assert.assertTrue(GlobalSettings.get("test.boolean.invalid-false", true));
    Assert.assertFalse(GlobalSettings.get("test.boolean.invalid-false", false));
    Assert.assertEquals("yes", GlobalSettings.get("test.boolean.invalid-yes"));
    Assert.assertTrue(GlobalSettings.get("test.boolean.invalid-yes", true));
    Assert.assertFalse(GlobalSettings.get("test.boolean.invalid-yes", false));
    Assert.assertEquals("no", GlobalSettings.get("test.boolean.invalid-no"));
    Assert.assertTrue(GlobalSettings.get("test.boolean.invalid-no", true));
    Assert.assertFalse(GlobalSettings.get("test.boolean.invalid-no", false));
    // Value is empty ("") -> default
    Assert.assertEquals("", GlobalSettings.get("test.boolean.empty"));
    Assert.assertTrue(GlobalSettings.get("test.boolean.empty", true));
    Assert.assertFalse(GlobalSettings.get("test.boolean.empty", false));
    // Value is undefined (null) -> default
    Assert.assertNull(GlobalSettings.get("test.boolean.undefined"));
    Assert.assertTrue(GlobalSettings.get("test.boolean.undefined", true));
    Assert.assertFalse(GlobalSettings.get("test.boolean.undefined", false));
  }

  /**
   * <int valid="123" toolarge="129999999999999999999999" invalid="not_an_int" empty=""/>
   */
  @Test
  public void testGet_Int() {
    // Value is a number
    Assert.assertEquals("123", GlobalSettings.get("test.int.valid"));
    Assert.assertEquals(123, GlobalSettings.get("test.int.valid", 777));
    // Value is larger than MAX_INTEGER
    Assert.assertEquals(777, GlobalSettings.get("test.int.toolarge", 777));
    // Value is not an integer
    Assert.assertEquals(777, GlobalSettings.get("test.int.invalid", 777));
    // Value is empty
    Assert.assertEquals("", GlobalSettings.get("test.int.empty"));
    Assert.assertEquals(777, GlobalSettings.get("test.int.empty", 777));
    Assert.assertEquals(777, GlobalSettings.get("test.int.empty", 777));
    // Value is undefined (null)
    Assert.assertNull(GlobalSettings.get("test.int.undefined"));
    Assert.assertEquals(777, GlobalSettings.get("test.int.undefined", 777));
    Assert.assertEquals(777, GlobalSettings.get("test.int.undefined", 777));
  }

  @Test
  public void testSetMode() {
    Assert.assertEquals("default", GlobalSettings.getMode());
    GlobalSettings.setMode("test");
    Assert.assertEquals("test", GlobalSettings.getMode());
    GlobalSettings.setMode("default");
    Assert.assertEquals("default", GlobalSettings.getMode());
  }

  @Test(expected = NullPointerException.class)
  public void testSetMode_Null() {
    GlobalSettings.setMode(null);
  }

  @Test
  public void testLoad_Override() {
    GlobalSettings.setMode("undefined"); // Loads without the override
    GlobalSettings.load();
    Assert.assertEquals("true", GlobalSettings.get("berlioz.xslt.cache"));
    Assert.assertEquals("default", GlobalSettings.get("app.location"));
    Assert.assertEquals("true", GlobalSettings.get("app.cache"));
    Assert.assertNull(GlobalSettings.get("app.name"));

    GlobalSettings.setMode("override1"); // Loads with the override (xml)
    GlobalSettings.load();
    Assert.assertEquals("false", GlobalSettings.get("berlioz.xslt.cache"));
    Assert.assertEquals("app1", GlobalSettings.get("app.name"));
    Assert.assertEquals("default", GlobalSettings.get("app.location"));
    Assert.assertEquals("false", GlobalSettings.get("app.cache"));

    GlobalSettings.setMode("override2"); // Loads with the override (properties)
    GlobalSettings.load();
    Assert.assertEquals("false", GlobalSettings.get("berlioz.xslt.cache"));
    Assert.assertEquals("app2", GlobalSettings.get("app.name"));
    Assert.assertEquals("default", GlobalSettings.get("app.location"));
    Assert.assertEquals("false", GlobalSettings.get("app.cache"));

  }

  @Test
  public void testLoad_Errors() {
    Assert.assertTrue(GlobalSettings.load());
    Assert.assertFalse(GlobalSettings.countProperties() == 0);

    GlobalSettings.setMode("empty");
    Assert.assertFalse(GlobalSettings.load());
    Assert.assertEquals(0, GlobalSettings.countProperties());

    GlobalSettings.setMode("invalid");
    Assert.assertFalse(GlobalSettings.load());
    Assert.assertEquals(0, GlobalSettings.countProperties());
  }

  @Test
  public void testLoad_Listeners() {
    final class NotifiableConfigListener implements ConfigListener {
      public int notifications = 0;
      @Override public void load() {
        this.notifications++;
      }
    };

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
    Assert.assertTrue(GlobalSettings.load());
    Assert.assertEquals(1, good.notifications);

    // Register bad listener and check that the good listener received notification and no exception is thrown
    GlobalSettings.registerListener(bad);
    Assert.assertTrue(GlobalSettings.load());
    Assert.assertEquals(2, good.notifications);

    // Check that listener is not notified if load fails
    GlobalSettings.setMode("empty");
    Assert.assertFalse(GlobalSettings.load());
    Assert.assertEquals(2,  good.notifications);
    GlobalSettings.removeAllListeners();
  }
}
