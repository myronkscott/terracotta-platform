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

import java.util.AbstractSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.terracotta.entity.ConcurrencyStrategy;
import org.terracotta.toolkit.barrier.BarrierConcurrency;

/**
 *
 */
public class ToolkitConcurrency implements ConcurrencyStrategy<ToolkitMessage>{
  
  private final int max = 1024;
  private final Map<String, ConcurrencyHelper> map;
  
  public ToolkitConcurrency(byte[] config) {
    Map<String, ConcurrencyHelper> sub = new HashMap<String, ConcurrencyHelper>();
    sub.put(ToolkitConstants.BARRIER_TYPE, new BarrierConcurrency());
    map = Collections.unmodifiableMap(sub);
  }

  @Override
  public int concurrencyKey(ToolkitMessage m) {
    switch (m.command()) {
      case OPERATION:
        String type = m.type();
        return map.get(type).concurrencyKey(m.name(), m.payload(), 1, max);
      default:
        return 1;
    }
  }

  @Override
  public Set<Integer> getKeysForSynchronization() {
    return new AbstractSet<Integer>() {
      @Override
      public Iterator<Integer> iterator() {
        return new Iterator<Integer>() {
          int current = 1;
          @Override
          public boolean hasNext() {
            return current < max;
          }

          @Override
          public Integer next() {
            return current++;
          }
        };
      }

      @Override
      public int size() {
        return max - 1;
      }
    };
  };
  
}
