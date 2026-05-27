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
 * @version Berlioz 0.13.0
 * @since Berlioz 0.13.0
 */
@Beta
public interface OutputWritable {

  /**
   * Writes this object to the given writer.
   *
   * @param out the writer to write to
   * @return the same writer, to allow chaining
   */
  OutputWriter toOutput(OutputWriter out);

}
