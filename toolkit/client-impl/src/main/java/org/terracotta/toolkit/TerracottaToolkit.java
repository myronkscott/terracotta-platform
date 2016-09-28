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

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observer;
import java.util.Queue;
import org.terracotta.entity.EndpointDelegate;
import org.terracotta.entity.EntityClientEndpoint;
import org.terracotta.entity.EntityResponse;
import org.terracotta.entity.MessageCodecException;
import org.terracotta.exception.EntityException;
import org.terracotta.toolkit.barrier.Barrier;
import org.terracotta.toolkit.barrier.BarrierConfig;
import org.terracotta.toolkit.list.ListConfig;
import org.terracotta.toolkit.map.MapConfig;
import org.terracotta.toolkit.observer.ObserverConfig;
import org.terracotta.toolkit.queue.QueueConfig;


public class TerracottaToolkit implements Toolkit, EndpointDelegate {
  
  private final EntityClientEndpoint<ToolkitMessage, ToolkitResponse> endpoint;
  private final Map<String, ToolkitReference> references = new HashMap<String, ToolkitReference>();
  private final ReferenceQueue<Object> queue = new ReferenceQueue<Object>();
  
  public TerracottaToolkit(EntityClientEndpoint<ToolkitMessage, ToolkitResponse> endpoint) {
    this.endpoint = endpoint;
  }
  
  private static String buildName(Class type, String name) {
    return type.getName() + ":" + name;
  }
  
  private void cleanup() {
    ToolkitReference ref = (ToolkitReference)queue.poll();
    while (ref != null) {
      String refName = ref.getName();
      release(refName);
    }
  }
  
  private synchronized void release(String name) {
    
  }
  
  private Object createType(Class type, String name) {
    if (type == Barrier.class) {
      return new TerracottaBarrier(endpoint, name);
    }
    return null;
  }
  
  private synchronized Object acquire(Class type, String name, boolean create) {
    cleanup();
    try {
      String tname = buildName(type, name);
      Object delegate = null;
      while (delegate != null) {
        delegate = createType(type, name);
        ToolkitReference placed = references.putIfAbsent(tname, new ToolkitReference(tname, delegate));
        if (placed != null) {
          delegate = placed.get();
        } else {
          ToolkitResponse resp = (create) ?                   
                  endpoint.beginInvoke().message(new CreateToolkitObject(type.getName(), name)).invoke().get()
          :
                  endpoint.beginInvoke().message(new GetToolkitObject(type.getName(), name)).invoke().get();
          switch (resp.result()) {
            case SUCCESS:
              break;
            case FAIL:
              throw new RuntimeException();
          }
        }
      }
    } catch (EntityException ee) {
      
    } catch (InterruptedException ie) {
      
    } catch (MessageCodecException me) {
      
    }
    return null;
  }
  
  private <T> T getToolkitObject(Class<T> type, String name) {
    return type.cast(acquire(type, name, false));
  }
  
  private <T> T createToolkitObject(Class<T> type, String name) {
    return type.cast(acquire(type, name, true));
  }

  @Override
  public Barrier createBarrier(String name, BarrierConfig parties) throws ToolkitObjectExists {
    return createToolkitObject(Barrier.class, name);
  }

  @Override
  public Barrier getBarrier(String name) {
    return getToolkitObject(Barrier.class, name);
  }

  @Override
  public Map createMap(String name, MapConfig config) throws ToolkitObjectExists {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public Map getMap(String name) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public Queue createQueue(String name, QueueConfig config) throws ToolkitObjectExists {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public Queue getQueue(String name) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public List createList(String name, ListConfig config) throws ToolkitObjectExists {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public List getList(String name) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public Observer createObserver(String name, ObserverConfig config) throws ToolkitObjectExists {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public Observer getObserver(String name) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void close() {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void handleMessage(EntityResponse er) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public byte[] createExtendedReconnectData() {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void didDisconnectUnexpectedly() {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  private class ToolkitReference extends WeakReference<Object> {
    private final String name;
    public ToolkitReference(String name, Object toolkitObject) {
      super(toolkitObject, queue);
      this.name = name;
    }
    
    String getName() {
      return name;
    }
  }
}
