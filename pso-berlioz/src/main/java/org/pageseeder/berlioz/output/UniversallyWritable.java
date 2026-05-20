package org.pageseeder.berlioz.output;

import org.pageseeder.berlioz.Beta;

@Beta
public interface UniversallyWritable {

  UniversalWriter toOutput(UniversalWriter out);

}
