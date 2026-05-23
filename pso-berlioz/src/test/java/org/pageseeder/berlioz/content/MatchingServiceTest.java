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

import org.junit.Assert;
import org.junit.Test;
import org.pageseeder.berlioz.furi.URIPattern;
import org.pageseeder.berlioz.furi.URIResolveResult;
import org.pageseeder.berlioz.furi.URIResolver;
import org.pageseeder.berlioz.generator.NoContent;

public final class MatchingServiceTest {

  private static Service buildService(String id) {
    return new Service.Builder()
        .id(id)
        .group("test")
        .rule(ServiceStatusRule.DEFAULT_RULE)
        .add(new NoContent())
        .build();
  }

  @Test(expected = NullPointerException.class)
  public void testConstructor_nullService() {
    URIPattern pattern = new URIPattern("/home");
    URIResolveResult result = new URIResolver("/home").resolve(pattern);
    new MatchingService(null, pattern, result);
  }

  @Test(expected = NullPointerException.class)
  public void testConstructor_nullPattern() {
    Service service = buildService("home");
    URIPattern pattern = new URIPattern("/home");
    URIResolveResult result = new URIResolver("/home").resolve(pattern);
    new MatchingService(service, null, result);
  }

  @Test(expected = NullPointerException.class)
  public void testConstructor_nullResult() {
    Service service = buildService("home");
    URIPattern pattern = new URIPattern("/home");
    new MatchingService(service, pattern, null);
  }

  @Test
  public void testGetters() {
    Service service = buildService("home");
    URIPattern pattern = new URIPattern("/home");
    URIResolveResult result = new URIResolver("/home").resolve(pattern);
    MatchingService match = new MatchingService(service, pattern, result);

    Assert.assertSame(service, match.service());
    Assert.assertSame(pattern, match.pattern());
    Assert.assertSame(result, match.result());
  }

  @Test
  public void testIsCacheable_cacheableGenerators() {
    Service service = buildService("home");
    URIPattern pattern = new URIPattern("/home");
    URIResolveResult result = new URIResolver("/home").resolve(pattern);
    MatchingService match = new MatchingService(service, pattern, result);

    // NoContent implements Cacheable
    Assert.assertTrue(match.isCacheable());
  }

  @Test
  public void testIsCacheable_nonCacheableGenerator() {
    // A generator that does NOT implement Cacheable
    ContentGenerator nonCacheable = (req, xml) -> {};
    Service service = new Service.Builder()
        .id("non-cacheable")
        .group("test")
        .rule(ServiceStatusRule.DEFAULT_RULE)
        .add(nonCacheable)
        .build();
    URIPattern pattern = new URIPattern("/test");
    URIResolveResult result = new URIResolver("/test").resolve(pattern);
    MatchingService match = new MatchingService(service, pattern, result);

    Assert.assertFalse(match.isCacheable());
  }
}
