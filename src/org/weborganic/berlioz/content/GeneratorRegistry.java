package org.weborganic.berlioz.content;

/**
 * A registry for content generators.
 * 
 * @deprecated
 * 
 * <p>Note: this class is not synchronized and must be synchronised externally.
 * 
 * @author Christophe Lauret
 * @version 20 May 2010
 */
public final class GeneratorRegistry extends ServiceRegistry {

  /**
   * Register the content generator.
   * 
   * @param generator the content generator to register.
   * @param pattern   the URL pattern to associate to this content generator.
   * @param method    the method for this url pattern.
   */
  public void register(ContentGenerator generator, String pattern, String method) {
    // preliminary checks
    if (generator == null) throw new IllegalArgumentException("No generator to register.");
    Service.Builder builder = new Service.Builder();
    builder.add(generator);
    builder.id(generator.getService());
    builder.group(generator.getArea());
    register(builder.build(), pattern, method);
  }

  /**
   * Register the content generator for the HTTP methods in use this registry only.
   * 
   * @param generator the content generator to register.
   * @param pattern   the URL pattern to associate to this content generator.
   */
  public void register(ContentGenerator generator, String pattern) {
    // preliminary checks
    if (generator == null) throw new IllegalArgumentException("No generator to register.");
    Service.Builder builder = new Service.Builder();
    builder.add(generator);
    builder.id(generator.getService());
    builder.group(generator.getArea());
    register(builder.build(), pattern);
  }

}
