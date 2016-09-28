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

import java.util.Set;
import org.terracotta.entity.ConcurrencyStrategy;

/**
 *
 */
public class ToolkitConcurrency implements ConcurrencyStrategy<ToolkitMessage>{

  @Override
  public int concurrencyKey(ToolkitMessage m) {
    switch (m.command()) {
      default:
        return 1;
    }
  }

  @Override
  public Set<Integer> getKeysForSynchronization() {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }
  
}
