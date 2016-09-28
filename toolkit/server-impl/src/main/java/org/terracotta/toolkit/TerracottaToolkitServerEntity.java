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

import java.util.HashMap;
import java.util.Map;
import org.terracotta.entity.ActiveServerEntity;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.PassiveSynchronizationChannel;


public class TerracottaToolkitServerEntity implements ActiveServerEntity<ToolkitMessage, ToolkitResponse> {
  private final Map<String, ServerHandler> objectSpace = new HashMap<>();
  @Override
  public void connected(ClientDescriptor cd) {

  }

  @Override
  public void disconnected(ClientDescriptor cd) {

  }

  @Override
  public ToolkitResponse invoke(ClientDescriptor cd, ToolkitMessage m) {
    switch(m.command()) {
      case CREATE:
        return createToolkitObject(cd, m);
      case GET:
        return referenceToolkitObject(cd, m);
      case RELEASE:
        return dropToolkitObject(cd, m);
      default:
        throw new RuntimeException();
    }
  }
  
  private ToolkitResponse dropToolkitObject(ClientDescriptor cd, ToolkitMessage m) {
    String name = buildName(m);
    ServerHandler handler = objectSpace.get(name);
    if (handler == null) {
      return fail();
    } else {
      int count = handler.dereference(cd);
      if (count == 0) {
        objectSpace.remove(name);
      }
      return success();
    }
  }  
  
  private ToolkitResponse referenceToolkitObject(ClientDescriptor cd, ToolkitMessage m) {
    String name = buildName(m);
    ServerHandler handler = objectSpace.get(name);
    if (handler == null) {
      return fail();
    } else {
      handler.reference(cd);
      return success();
    }
  }  
  
  private ToolkitResponse createToolkitObject(ClientDescriptor cd, ToolkitMessage m) {
    String name = buildName(m);
    ServerHandler handler = objectSpace.putIfAbsent(name, new ServerHandler(cd) {
      @Override
      ToolkitResponse handleMessage(byte[] raw) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
      }
    });
    if (handler != null) {
      return fail();
    } else {
      return success();
    }
  }

  private static ToolkitResponse success() {
    return new ToolkitResponse() {
      @Override
      public ToolkitResult result() {
        return ToolkitResult.SUCCESS;
      }
    };
  }
  
  private static ToolkitResponse fail() {
    return new ToolkitResponse() {
      @Override
      public ToolkitResult result() {
        return ToolkitResult.FAIL;
      }
    };
  }
  
  private static String buildName(ToolkitMessage m) {
    return m.type() + ":" + m.name();
  }

  @Override
  public void handleReconnect(ClientDescriptor cd, byte[] bytes) {

  }

  @Override
  public void synchronizeKeyToPassive(PassiveSynchronizationChannel<ToolkitMessage> psc, int i) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void createNew() {

  }

  @Override
  public void loadExisting() {

  }

  @Override
  public void destroy() {

  }

}
