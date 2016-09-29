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

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.terracotta.entity.ActiveServerEntity;
import org.terracotta.entity.ClientCommunicator;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.MessageCodecException;
import org.terracotta.entity.PassiveSynchronizationChannel;
import org.terracotta.toolkit.barrier.BarrierConfig;
import org.terracotta.toolkit.barrier.BarrierServerHandler;


public class TerracottaToolkitServerEntity implements ActiveServerEntity<ToolkitMessage, ToolkitResponse> {
  private final Map<String, ServerHandler> objectSpace = new ConcurrentHashMap<>();
  private final ClientCommunicator communicator;

  public TerracottaToolkitServerEntity(ClientCommunicator communicator, byte[] config) {
    this.communicator = communicator;
  }
  
  @Override
  public void connected(ClientDescriptor cd) {

  }

  @Override
  public void disconnected(ClientDescriptor cd) {
    for (Map.Entry<String, ServerHandler> handler : objectSpace.entrySet()) {
      if (handler.getValue().dereference(cd) == 0) {
        objectSpace.remove(handler.getKey());
      }
    }
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
      case OPERATION:
        return runOperation(cd, m);
      default:
        throw new RuntimeException();
    }
  }
  
  private ToolkitResponse runOperation(ClientDescriptor cd, ToolkitMessage m) {
    String name = buildName(m);
    ServerHandler handler = objectSpace.get(name);
    if (handler == null) {
      return fail();
    } else {
      try {
        return handler.handleMessage(cd, m.payload());
      } catch (MessageCodecException codec) {
        return fail();
      }
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
    ServerHandler handler = objectSpace.putIfAbsent(name, translate(m.type(), m.name(), cd, m));
    if (handler != null) {
      return fail();
    } else {
      return success();
    }
  }
  
  private ServerHandler translate(String type, String name, ClientDescriptor descriptor, ToolkitMessage m) {
    if (m.type().equals(ToolkitConstants.BARRIER_TYPE)) {
      return new BarrierServerHandler(type, name, new BarrierConfig(m.payload()), communicator, descriptor);
    }
    return null;
  }

  private static ToolkitResponse success() {
    return new ToolkitResponse() {
      @Override
      public ToolkitResult result() {
        return ToolkitResult.SUCCESS;
      }

      @Override
      public byte[] payload() {
        return new byte[0];
      }

      @Override
      public String type() {
        return "";
      }

      @Override
      public String name() {
        return "";
      }
    };
  }
  
  private static ToolkitResponse fail() {
    return new ToolkitResponse() {
      @Override
      public ToolkitResult result() {
        return ToolkitResult.FAIL;
      }
      

      @Override
      public byte[] payload() {
        return new byte[0];
      }   
      

      @Override
      public String type() {
        return "";
      }

      @Override
      public String name() {
        return "";
      }      
    };
  }
  
  private static String buildName(ToolkitMessage m) {
    return buildName(m.type(), m.name());
  }
  
  private static String buildName(String type, String name) {
    return type + ":" + name;
  }

  @Override
  public void handleReconnect(ClientDescriptor cd, byte[] bytes) {
    Collection<ToolkitReconnectData> reconnect = ToolkitCodec.decodeReconnectData(bytes);
    for (ToolkitReconnectData trd : reconnect) {
      ServerHandler handler = objectSpace.get(buildName(trd.getType(), trd.getName()));
      if (handler != null) {
        try {
          handler.handleReconnect(cd, trd.getPayload());
        } catch (MessageCodecException codec) {
          throw new RuntimeException(codec);
        }
      }
    }
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
