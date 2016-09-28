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
package org.terracotta.toolkit;

import org.terracotta.entity.EntityClientEndpoint;
import org.terracotta.entity.EntityClientService;
import org.terracotta.entity.MessageCodec;

/**
 */
public class TerracottaToolkitEntityClientService implements EntityClientService<Toolkit, ToolkitConfig, ToolkitMessage, ToolkitResponse> {

  @Override
  public boolean handlesEntityType(Class<Toolkit> type) {
    if (type == Toolkit.class) {
      return true;
    }
    return false;
  }

  @Override
  public byte[] serializeConfiguration(ToolkitConfig c) {
    return new byte[0];
  }

  @Override
  public ToolkitConfig deserializeConfiguration(byte[] bytes) {
    return new ToolkitConfig();
  }

  @Override
  public Toolkit create(EntityClientEndpoint<ToolkitMessage, ToolkitResponse> ece) {
    return new TerracottaToolkit(ece);
  }

  @Override
  public MessageCodec<ToolkitMessage, ToolkitResponse> getMessageCodec() {
    return new ToolkitCodec();
  }

}
