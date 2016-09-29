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

import com.tc.classloader.PermanentEntity;
import org.terracotta.entity.ActiveServerEntity;
import org.terracotta.entity.BasicServiceConfiguration;
import org.terracotta.entity.ClientCommunicator;
import org.terracotta.entity.ConcurrencyStrategy;
import org.terracotta.entity.EntityServerService;
import org.terracotta.entity.MessageCodec;
import org.terracotta.entity.PassiveServerEntity;
import org.terracotta.entity.ServiceRegistry;
import org.terracotta.entity.SyncMessageCodec;


@PermanentEntity(names = {ToolkitConstants.STANDARD_TOOLKIT}, type = ToolkitConstants.TOOLKIT_TYPE)
public class TerracottaToolkitEntityServerService implements EntityServerService<ToolkitMessage, ToolkitResponse> {
  
  @Override
  public long getVersion() {
    return 1;
  }

  @Override
  public boolean handlesEntityType(String string) {
    if (string.equals("org.terracotta.toolkit.Toolkit")) {
      return true;
    }
    return false;
  }

  @Override
  public ActiveServerEntity<ToolkitMessage, ToolkitResponse> createActiveEntity(ServiceRegistry sr, byte[] bytes) {
    return new TerracottaToolkitServerEntity(sr.getService(new BasicServiceConfiguration<>(ClientCommunicator.class)));
  }

  @Override
  public PassiveServerEntity<ToolkitMessage, ToolkitResponse> createPassiveEntity(ServiceRegistry sr, byte[] bytes) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public ConcurrencyStrategy<ToolkitMessage> getConcurrencyStrategy(byte[] bytes) {
   return new ToolkitConcurrency();
  }

  @Override
  public MessageCodec<ToolkitMessage, ToolkitResponse> getMessageCodec() {
    return new ToolkitCodec();
  }

  @Override
  public SyncMessageCodec<ToolkitMessage> getSyncMessageCodec() {
    return null;
  }

}
