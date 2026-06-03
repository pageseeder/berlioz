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
package org.pageseeder.berlioz.xslt;

import javax.xml.transform.TransformerException;

/**
 * Wraps a {@link TransformerException} to carry the associated {@link XsltErrorCollector},
 * so that collected warnings and errors are available to error reporters.
 *
 * @author Christophe Lauret
 *
 * @version 0.13.1
 * @since 0.13.1
 */
public final class XsltExceptionWrapper extends TransformerException {

  private static final long serialVersionUID = -7816677212503520650L;

  private final transient XsltErrorCollector collector;

  /**
   * @param ex        the wrapped transformer exception.
   * @param collector the collected XSLT errors associated with this exception.
   */
  public XsltExceptionWrapper(TransformerException ex, XsltErrorCollector collector) {
    super(ex);
    this.collector = collector;
  }

  /**
   * @return the collected XSLT errors associated with this exception.
   */
  public XsltErrorCollector collector() {
    return this.collector;
  }

}
