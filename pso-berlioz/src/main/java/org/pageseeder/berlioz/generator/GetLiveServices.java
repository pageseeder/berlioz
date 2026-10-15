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
package org.pageseeder.berlioz.generator;

import org.pageseeder.berlioz.Beta;

/**
 * Deprecated forwarding alias of {@link ListRuntimeServices}.
 *
 * <p>Retained so that existing {@code services.xml} generator references naming
 * {@code org.pageseeder.berlioz.generator.GetLiveServices} keep resolving without an
 * application change. New configuration should reference {@link ListRuntimeServices} directly.
 * This alias will be removed at 1.0.
 *
 * @author Christophe Lauret
 *
 * @version 0.14.3
 * @since 0.9.3
 *
 * @deprecated Use {@link ListRuntimeServices} instead; this alias is retained until 1.0.
 */
@Beta
@Deprecated
public class GetLiveServices extends ListRuntimeServices {
}
