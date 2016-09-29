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
import java.util.ArrayList;
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
  
  private static String buildName(String type, String name) {
    return type + ":" + name;
  }
  
  private void cleanup() {
    ToolkitReference ref = (ToolkitReference)queue.poll();
    while (ref != null) {
      release(ref.getType(), ref.getName());
    }
  }
  
  private synchronized void release(String type, String name) {
    ToolkitReference ref = references.remove(buildName(type, name));
    if (ref != null) {
      try {
        ToolkitResponse response = endpoint.beginInvoke().message(new ReleaseToolkitObject(type, name)).invoke().get();
      } catch (EntityException ee) {
        ee.printStackTrace();
      } catch (InterruptedException ie) {
        ie.printStackTrace();
      } catch (MessageCodecException me) {
        me.printStackTrace();
      }
    }
  }
  
  public void release(ToolkitObject object) {
    release(object.getType(), object.getName());
  }
  
  private <T extends ToolkitObject,C> T createType(Class<T> type, String name, C config) {
    if (type == Barrier.class) {
      return type.cast(new TerracottaBarrier(this, endpoint, type.getName(), name, (BarrierConfig)config));
    }
    return null;
  }
  
  private synchronized <T extends ToolkitObject,C> T acquire(Class<T> type, String name, C create) {
    cleanup();
    try {
      String tname = buildName(type.getName(), name);
      ToolkitObject delegate = null;
      while (delegate == null) {
        ToolkitReference placed = references.get(tname);
        if (create != null) {
          delegate = createType(type, name, create);
          placed = references.putIfAbsent(tname, new ToolkitReference(delegate));
        }
        if (placed != null) {
          delegate = placed.get();
        } else {
          ToolkitResponse resp = (create != null) ?                   
                  endpoint.beginInvoke().message(new CreateToolkitObject(type.getName(), name, ((BarrierConfig)create).toRaw())).invoke().get()
          :
                  endpoint.beginInvoke().message(new GetToolkitObject(type.getName(), name)).invoke().get();
          switch (resp.result()) {
            case SUCCESS:
              return type.cast(delegate);
            case FAIL:
              return null;
          }
        }
      }
      return type.cast(delegate);
    } catch (EntityException ee) {
      throw new RuntimeException(ee);
    } catch (InterruptedException ie) {
      throw new RuntimeException(ie);
    } catch (MessageCodecException me) {
      throw new RuntimeException(me);
    }
  }
  
  private <T extends ToolkitObject> T getToolkitObject(Class<T> type, String name) {
    return type.cast(acquire(type, name, null));
  }
  
  private <T extends ToolkitObject, C> T createToolkitObject(Class<T> type, String name, C config) {
    return type.cast(acquire(type, name, config));
  }

  @Override
  public Barrier createBarrier(String name, BarrierConfig parties) throws ToolkitObjectExists {
    return createToolkitObject(Barrier.class, name, parties);
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
    ToolkitResponse response = (ToolkitResponse)er;
    ToolkitReference ref = references.get(buildName(response.type(), response.name()));
    ToolkitObject object = (ref != null) ? ref.get() : null;
    if (object != null) {
      object.handleServerMessage(response);
    }
  }

  @Override
  public byte[] createExtendedReconnectData() {
    List<ToolkitReconnectData> list = new ArrayList<ToolkitReconnectData>();
    for (ToolkitReference refs :references.values()) {
      ToolkitObject object = refs.get();
      if (object != null) {
        list.add(new ToolkitReconnectData(object.getType(), object.getName(), object.createReconnectData()));
      }
    }
    return ToolkitCodec.encodeReconnectData(list);
  }

  @Override
  public void didDisconnectUnexpectedly() {
    for (ToolkitReference refs :references.values()) {
      ToolkitObject object = refs.get();
      if (object != null) {
        object.didDisconnectUnexpectedly();
      }
    }
  }

  private class ToolkitReference extends WeakReference<ToolkitObject> {
    private final String name;
    private final String type;
    public ToolkitReference(ToolkitObject toolkitObject) {
      super(toolkitObject, queue);
      this.name = toolkitObject.getName();
      this.type = toolkitObject.getType();
    }
    
    String getName() {
      return name;
    }
    
    String getType() {
      return type;
    }
  }
}
