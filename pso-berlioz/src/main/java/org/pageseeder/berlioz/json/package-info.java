/*
 * Copyright 2020 Allette Systems (Australia)
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
 * JSON writer abstraction with provider-detected implementations (Jackson, Gson, JSONP, builtin).
 *
 * <p>Use {@link org.pageseeder.berlioz.json.Json#newWriter} to obtain a {@link org.pageseeder.berlioz.json.JsonWriter}
 * backed by the first JSON library found on the classpath.</p>
 *
 * @see <a href="https://www.rfc-editor.org/rfc/rfc8259">RFC 8259 – The JSON Data Interchange Format</a>
 */
@org.jspecify.annotations.NullMarked
package org.pageseeder.berlioz.json;
