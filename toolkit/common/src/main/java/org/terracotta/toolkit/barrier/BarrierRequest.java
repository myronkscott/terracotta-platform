/*
 * Copyright Terracotta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.toolkit.barrier;

import java.util.UUID;
import org.terracotta.entity.EntityMessage;

/**
 *
 */
public class BarrierRequest implements EntityMessage {
  private final UUID uuid;

  public BarrierRequest(UUID uuid) {
    this.uuid = uuid;
  }

  public UUID getUuid() {
    return uuid;
  }
}
