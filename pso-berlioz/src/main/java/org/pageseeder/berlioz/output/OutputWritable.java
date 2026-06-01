package org.pageseeder.berlioz.output;

import org.pageseeder.berlioz.Beta;

/**
 * Implemented by objects that can write their state to an {@link OutputWriter}.
 *
 * <p>This interface follows a visitor-like pattern where the object drives its own
 * serialization, remaining agnostic of whether the target format is XML or JSON.
 *
 * @author Christophe Lauret
 *
 * @version 0.13.0
 * @since 0.13.0
 */
@Beta
public interface OutputWritable {

  /**
   * Writes this object to the given writer.
   *
   * @param out the writer to write to
   * @return the same writer, to allow chaining
   */
  OutputWriter writeTo(OutputWriter out);

}
