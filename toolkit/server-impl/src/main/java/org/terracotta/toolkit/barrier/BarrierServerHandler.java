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

import java.nio.ByteBuffer;
import org.terracotta.toolkit.*;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.terracotta.entity.ClientCommunicator;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.MessageCodecException;

/**
 *
 */
public class BarrierServerHandler extends ServerHandler {
  
  private final ClientCommunicator communicator;

  private long generation;
  private final int parties;
  private final Map<UUID, ClientDescriptor> mapper = new ConcurrentHashMap<>();
  private final static BarrierCodec codec = new BarrierCodec();
  
  private volatile boolean reconnectBarrier = false;

  public BarrierServerHandler(String type, String name, BarrierConfig config, ClientCommunicator communicator, ClientDescriptor creator) {
    super(type, name, creator);
    this.communicator = communicator;
    this.parties = config.parties();
  }

  @Override
  public ToolkitResponse handleMessage(ClientDescriptor clientDescriptor, byte[] raw) throws MessageCodecException {
    BarrierRequest request = codec.decodeMessage(raw);
    
// the waiter token identifies a specific wait
    UUID waiter = request.getUuid();
    if (mapper.put(waiter, clientDescriptor) != null) {
      throw new AssertionError();
    }
// if the map is full, wake everyone else up clear the map and don't wait on the client triggering the wake
    if (mapper.size() == parties) {
      for (ClientDescriptor other : mapper.values()) {
// update generation on the clients 
        try {
          communicator.sendNoResponse(other, wrap(true, new BarrierResponse(true, generation, -1)));
        } catch (MessageCodecException e) {
          throw new AssertionError();
        }
      }
//  advance the generation and clear the map
      this.generation += 1;
      mapper.clear();
      return wrap(true, new BarrierResponse(false, 0L, this.mapper.size()));
    } else {
//  tell the client the current generation to wait on        
      return wrap(true, new BarrierResponse(false, this.generation, this.mapper.size()));
    }  
  }
  
  @Override
  public void handleReconnect(ClientDescriptor clientDescriptor, byte[] extendedReconnectData) {
// This is one of our clients reconnecting and sending us what they think the generation should be.
    if (reconnectBarrier) {
// the reconnect barrier is to look for a specific implementation error where the reconnect comes in 
// after an invoke.  This is strictly forbidden by the model
      throw new AssertionError();
    }
    ByteBuffer data = ByteBuffer.wrap(extendedReconnectData);
    if (extendedReconnectData.length > 8) {
// client is waiting on a token, put it in the map.  it may already be there.
      while (data.remaining() > 8) {
        UUID waiter = new UUID(data.getLong(), data.getLong());
        mapper.put(waiter, clientDescriptor);
      }
    }
// find the generation.
    long clientGeneration = ByteBuffer.wrap(extendedReconnectData).getLong();
    if (generation < clientGeneration + 1) {
// if this is the case, all previously reporting generations are stale.  wake them all up.
      for (ClientDescriptor other : mapper.values()) {
        if (!other.equals(clientDescriptor)) {
          try {
            communicator.sendNoResponse(other, wrap(true, new BarrierResponse(true, this.generation, -1)));
          } catch (MessageCodecException e) {
            throw new AssertionError();
          }
        }
      }
    }
    this.generation = Long.max(this.generation, clientGeneration + 1);
  }
  
  private ToolkitResponse wrap(boolean success, BarrierResponse response) {
    return new ToolkitResponse() {
      @Override
      public ToolkitResult result() {
        return success ? ToolkitResult.SUCCESS : ToolkitResult.FAIL;
      }

      @Override
      public byte[] payload() {
        try {
          return codec.encodeResponse(response);
        } catch (MessageCodecException m) {
          throw new RuntimeException(m);
        }
      }

      @Override
      public String type() {
        return getType();
      }

      @Override
      public String name() {
        return getName();
      }
    };
  }
}
