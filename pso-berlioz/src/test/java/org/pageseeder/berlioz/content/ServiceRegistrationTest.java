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

import java.net.MalformedURLException;
import java.net.URL;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.pageseeder.berlioz.furi.URIPattern;
import org.pageseeder.berlioz.http.HttpMethod;

class ServiceRegistrationTest {

  @Test
  void testAccessorsAndToString() throws MalformedURLException {
    Service service = new Service.Builder().id("home").group("default").rule(ServiceStatusRule.DEFAULT_RULE).build();
    URIPattern pattern = new URIPattern("/home");
    URL url = new URL("jar:file:/opt/apps/my-overlay.jar!/META-INF/berlioz/services.xml");
    ServiceOrigin origin = ServiceOrigin.forClasspathResource(url);

    ServiceRegistration registration = new ServiceRegistration(service, HttpMethod.GET, pattern, origin);

    Assertions.assertEquals(service, registration.service());
    Assertions.assertEquals(HttpMethod.GET, registration.method());
    Assertions.assertEquals(pattern, registration.pattern());
    Assertions.assertEquals(origin, registration.origin());
    Assertions.assertTrue(registration.toString().contains("my-overlay.jar"));
  }

}
