/*
 * Copyright 2026 Allette Systems (Australia)
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
package org.pageseeder.berlioz.content;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class UpstreamExceptionTest {

  @Test
  void constructor_message_noUpstreamService() {
    UpstreamException ex = new UpstreamException("connection refused");
    Assertions.assertEquals("connection refused", ex.getMessage());
    Assertions.assertNull(ex.getUpstreamService());
    Assertions.assertNull(ex.getCause());
  }

  @Test
  void constructor_messageCause_noUpstreamService() {
    Throwable cause = new RuntimeException("timeout");
    UpstreamException ex = new UpstreamException("connection refused", cause);
    Assertions.assertEquals("connection refused", ex.getMessage());
    Assertions.assertNull(ex.getUpstreamService());
    Assertions.assertSame(cause, ex.getCause());
  }

  @Test
  void constructor_messageAndService_returnsService() {
    UpstreamException ex = new UpstreamException("not available", "search-api");
    Assertions.assertEquals("not available", ex.getMessage());
    Assertions.assertEquals("search-api", ex.getUpstreamService());
    Assertions.assertNull(ex.getCause());
  }

  @Test
  void constructor_messageServiceAndCause_allFieldsSet() {
    Throwable cause = new RuntimeException("404");
    UpstreamException ex = new UpstreamException("not available", "PageSeeder", cause);
    Assertions.assertEquals("not available", ex.getMessage());
    Assertions.assertEquals("PageSeeder", ex.getUpstreamService());
    Assertions.assertSame(cause, ex.getCause());
  }

  @Test
  void fillInStackTrace_stackTraceIsEmpty() {
    UpstreamException ex = new UpstreamException("down");
    Assertions.assertEquals(0, ex.getStackTrace().length);
  }

}
