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
/**
 * RFC 9457 Problem Details infrastructure for Berlioz error responses.
 *
 * <p>Key types:</p>
 * <ul>
 *   <li>{@link org.pageseeder.berlioz.error.HttpException} — abstract base for HTTP short-circuit signals</li>
 *   <li>{@link org.pageseeder.berlioz.error.InvalidParameterException} — 400 Bad Request signal</li>
 *   <li>{@link org.pageseeder.berlioz.error.UpstreamException} — 502 Bad Gateway signal</li>
 *   <li>{@link org.pageseeder.berlioz.error.ProblemDetails} — immutable RFC 9457 problem object</li>
 *   <li>{@link org.pageseeder.berlioz.error.ProblemExtension} — structured problem extension contract</li>
 *   <li>{@link org.pageseeder.berlioz.error.Problems} — framework factory for all problem types</li>
 *   <li>{@link org.pageseeder.berlioz.error.DetailLevel} — verbosity control for error responses</li>
 *   <li>{@link org.pageseeder.berlioz.error.ExceptionDetail} — structured exception serialization</li>
 * </ul>
 */
@org.jspecify.annotations.NullMarked
package org.pageseeder.berlioz.error;
