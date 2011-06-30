package org.weborganic.berlioz;

/**
 * An ID for errors to help with error handling and diagnostic.
 * 
 * <p>Error IDs atsrting with "bz" are reserved by Berlioz.
 * 
 * <p>Note: these are different and complementary to HTTP response code.
 * 
 * @author Christophe Lauret
 * @version 30 June 2011
 */
@Beta public interface ErrorID {

  String toString();

}
