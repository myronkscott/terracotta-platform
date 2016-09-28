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

import java.util.HashSet;
import java.util.Set;
import org.terracotta.entity.ClientDescriptor;

/**
 *
 */
public abstract class ServerHandler {
  
  private int referenceCount = 1;
  private final ClientDescriptor creator;
  private final Set<ClientDescriptor> refers = new HashSet<>();

  public ServerHandler(ClientDescriptor creator) {
    this.creator = creator;
    refers.add(creator);
  }
  
  abstract ToolkitResponse handleMessage(byte[] raw);
  
  public int reference(ClientDescriptor ref) {
    if (refers.add(ref)) {
      referenceCount += 1;
    }
    if (referenceCount != refers.size()) {
      throw new AssertionError("refcount != set " + referenceCount + " " + refers.size());
    }
    return referenceCount;
  }
  
  public int dereference(ClientDescriptor ref) {
    if (refers.remove(ref)) {
      referenceCount -= 1;
    }
    if (referenceCount != refers.size()) {
      throw new AssertionError("refcount != set " + referenceCount + " " + refers.size());
    }
    return referenceCount;
  }
}
