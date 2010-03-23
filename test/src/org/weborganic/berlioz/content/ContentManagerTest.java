package org.weborganic.berlioz.content;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.weborganic.berlioz.BerliozException;
import org.weborganic.berlioz.GlobalSettings;
import org.weborganic.berlioz.test.TestUtils;

import com.topologi.diffx.xml.XMLWriter;

import junit.framework.TestCase;

/**
 * A test case for the Berlioz entity resolver.
 * 
 * @author Christophe Lauret (Weborganic)
 * @version 29 October 2009
 */
public class ContentManagerTest extends TestCase {

  static {
    BasicConfigurator.configure();
    Logger logger = Logger.getRootLogger();
    logger.setLevel(Level.DEBUG);
  }

  /**
   * Where the test data can be found.
   */
  private static final File DATA = TestUtils.getDataDirectory(ContentManager.class);

  /**
   * File filter for visible directories.
   */
  private static final FileFilter VISIBLE_DIRECTORY = new FileFilter() {
    public boolean accept(File f) {
      return f.isDirectory() && !f.isHidden();
    }
  };

  @Override
  protected void setUp() throws Exception {
    ContentManager.clear();
  }

  /**
   * Check that all configurations can be loaded using the loadGenerators method.
   */
  public void testLoadGenerators() throws BerliozException {
    // Iterate over the various repositories
    for (File r : DATA.listFiles(VISIBLE_DIRECTORY)) {
      GlobalSettings.setRepository(r);
      ContentManager.clear();
      ContentManager.load();
    }
  }

  /**
   * 
   */
  public void testGetInstance_A0() {
    GlobalSettings.setRepository(new File(DATA, "A0"));
    // test the home page
    ContentGenerator home = ContentManager.getInstance("/home");
    assertNotNull(home);
    assertEquals(NoContent.class, home.getClass());
    // test the custom page 
    ContentGenerator custom = ContentManager.getInstance("/custom");
    assertNotNull(custom);
    assertEquals(Custom.class, custom.getClass());
    // test the custom page 
    ContentGenerator unknown = ContentManager.getInstance("/unknown");
    assertNull(unknown);
  }

  /**
   * 
   */
  public void testGetInstance_S0() {
    GlobalSettings.setRepository(new File(DATA, "S0"));
    // test the home page
    ContentGenerator home = ContentManager.getInstance("/home");
    assertNotNull(home);
    assertEquals(NoContent.class, home.getClass());
    // test the custom page 
    ContentGenerator custom = ContentManager.getInstance("/custom");
    assertNotNull(custom);
    assertEquals(Custom.class, custom.getClass());
    // test a non-matching page
    ContentGenerator unknown = ContentManager.getInstance("/unknown");
    assertNull(unknown);
    // test a page with pattern
    ContentGenerator user1 = ContentManager.getInstance("/user/123");
    ContentGenerator user2 = ContentManager.getInstance("/user/abc");
    assertNotNull(user2);
    assertEquals(NoContent.class, user1.getClass());
    assertEquals(NoContent.class, user2.getClass());
  }

  /**
   * A basic content generator for testing.
   */  
  public static class Custom extends ContentGeneratorBase implements ContentGenerator {

    public void manage(ContentRequest req, XMLWriter xml) throws BerliozException, IOException {
    }

    public void process(ContentRequest req, XMLWriter xml) throws BerliozException, IOException {
    }
  }

}
