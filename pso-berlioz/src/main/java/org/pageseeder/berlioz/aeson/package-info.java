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
/**
 * Aeson — XML-to-JSON serialization for Berlioz.
 *
 * <p>The central entry point is {@link org.pageseeder.berlioz.aeson.JSONResult}, which is a
 * {@code SAXResult} that converts SAX events into JSON output.  The conversion is driven by
 * {@link org.pageseeder.berlioz.aeson.JSONSerializer} and writes through the canonical
 * {@link org.pageseeder.berlioz.json.Json} provider selection used by the rest of Berlioz.
 *
 * <p>JSON type hints ({@code json:boolean}, {@code json:number}, {@code json:null}) are
 * controlled via namespace-qualified attributes using the namespace URI
 * {@value org.pageseeder.berlioz.aeson.JSONSerializer#NS_URI}.
 *
 * @see <a href="https://www.rfc-editor.org/rfc/rfc8259">RFC 8259 — The JavaScript Object Notation (JSON) Data Interchange Format</a>
 */
@org.jspecify.annotations.NullMarked
package org.pageseeder.berlioz.aeson;
