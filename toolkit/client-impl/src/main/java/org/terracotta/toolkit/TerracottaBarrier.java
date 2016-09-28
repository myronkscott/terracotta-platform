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

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.terracotta.entity.EntityClientEndpoint;
import org.terracotta.toolkit.barrier.Barrier;

/**
 *
 */
public class TerracottaBarrier implements Barrier {
  private final String name;
  private final String type;
  private final EntityClientEndpoint<ToolkitMessage, ToolkitResponse> endpoint;
  private final TerracottaToolkit toolkit;
  private final ReadWriteLock closeLock;
  private boolean closed = false;

  public TerracottaBarrier(TerracottaToolkit owner, EntityClientEndpoint<ToolkitMessage, ToolkitResponse> endpoint, String type, String name) {
    this.toolkit = owner;
    this.name = name;
    this.type = type;
    this.endpoint = endpoint;
    this.closeLock = new ReentrantReadWriteLock();
  }

  @Override
  public String getType() {
    return this.type;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public int getParties() {
    Lock lock = closeLock.readLock();
    lock.lock();
    try {
      if (!closed) {
        
      } else {
        throw new IllegalStateException();
      }
    } finally {
      lock.unlock();
    }
    return 0;
  }

  @Override
  public int await() throws InterruptedException {
    Lock lock = closeLock.readLock();
    lock.lock();
    try {
      if (!closed) {
        
      } else {
        throw new IllegalStateException();
      }
    } finally {
      lock.unlock();
    }
    return 0;
  }

  @Override
  public void close() throws Exception {
    Lock lock = closeLock.writeLock();
    lock.lock();
    try {
      if (!closed) {
        closed = true;
        toolkit.release(this);
      }
    } finally {
      lock.unlock();
    }
  }
}
