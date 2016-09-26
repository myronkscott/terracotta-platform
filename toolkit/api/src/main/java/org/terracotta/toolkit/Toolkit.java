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

import java.util.List;
import java.util.Map;
import java.util.Observer;
import java.util.Queue;
import org.terracotta.connection.entity.Entity;
import org.terracotta.toolkit.barrier.Barrier;
import org.terracotta.toolkit.barrier.BarrierConfig;
import org.terracotta.toolkit.list.ListConfig;
import org.terracotta.toolkit.map.MapConfig;
import org.terracotta.toolkit.observer.ObserverConfig;
import org.terracotta.toolkit.queue.QueueConfig;

/**
 *  Toolkit of utility objects in a clustered environment.  
 * 
 *  Lifecycling concerns - Structures created under the same name as an object 
 *   that already exists will result @see ToolkitObjectExists exception.
 * 
 *  Toolkit objects are deleted as soon as they are no longer referenced on a 
 *   client.
 */
public interface Toolkit  extends Entity {
  public static final long VERSION = 1;
  
  Barrier createBarrier(String name, BarrierConfig parties) throws ToolkitObjectExists;
  
  Barrier getBarrier(String name);
    
  Map createMap(String name, MapConfig config) throws ToolkitObjectExists;
  
  Map getMap(String name);
    
  Queue createQueue(String name, QueueConfig config) throws ToolkitObjectExists;
  
  Queue getQueue(String name);
  
  List createList(String name, ListConfig config) throws ToolkitObjectExists;
  
  List getList(String name);
  
  Observer createObserver(String name, ObserverConfig config) throws ToolkitObjectExists;
  
  Observer getObserver(String name);
  
  
}
