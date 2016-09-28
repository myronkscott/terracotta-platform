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
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.terracotta.entity.EntityClientEndpoint;
import org.terracotta.entity.InvokeFuture;
import org.terracotta.entity.MessageCodecException;
import org.terracotta.exception.EntityException;
import org.terracotta.toolkit.barrier.Barrier;
import org.terracotta.toolkit.barrier.BarrierCodec;
import org.terracotta.toolkit.barrier.BarrierConfig;
import org.terracotta.toolkit.barrier.BarrierRequest;
import org.terracotta.toolkit.barrier.BarrierResponse;

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
  private final int parties;
  private long generation = 0;
  private final Set<UUID> waitTokens = new HashSet<UUID>();
  private final OperationTarget<BarrierRequest, BarrierResponse> opTar;
  
  
  public TerracottaBarrier(TerracottaToolkit owner, EntityClientEndpoint<ToolkitMessage, ToolkitResponse> endpoint, String type, String name, BarrierConfig parties) {
    this.toolkit = owner;
    this.name = name;
    this.type = type;
    this.endpoint = endpoint;
    this.closeLock = new ReentrantReadWriteLock();
    this.parties = parties.parties();
    this.opTar = new OperationTarget<BarrierRequest, BarrierResponse>(type, name, new BarrierCodec());
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
    return parties;
  }

  @Override
  public int await() throws InterruptedException {
    Lock lock = closeLock.readLock();
    lock.lock();
    try {
      if (!closed) {
        UUID token = UUID.randomUUID();
        long waitGeneration = 0;
        int id = 0;
        try {
          InvokeFuture<ToolkitResponse> future = endpoint.beginInvoke().message(opTar.target(new BarrierRequest(token))).invoke();

          BarrierResponse response = opTar.process(future.get());
          waitGeneration = response.getGeneration();
          id = response.getWaitCount();
        } catch (EntityException e) {
          // We have no handling for this in this entity.
          throw new RuntimeException(e);
        } catch (MessageCodecException e) {
          throw new RuntimeException(e);
        }
        // NOTE:  We can't synchronize this entire method because that would cause an intermittent deadlock on the above
        // "future.get()" call and we only actually need it around this generation check wait loop.
        // The other side of the deadlock is in handleMessage() being synchronized:  if we are blocked in the future.get() (which
        // is an unrelated block point - not related to synchronization, just waiting on response), we can't get the monitor
        // to handleMessage.  Without another thread to deliver the response to the future, we will never break this deadlock.
        // The problem may only happen in the in-process framework since multiple threads handling messages back from the server
        // would alleviate the issue (although we shouldn't depend on that, if we can help it).
        synchronized(this) {
    // the wait token is set so the server knows about which wait it knows about.
          waitTokens.add(token);
          try {
    // wait until the waitGeneratio is the same as the generation on the server
            while(generation < waitGeneration) {
              if (generation < 0) {
                throw new IllegalStateException("broken barrier");
              }
              wait();
            }
          } finally {
            waitTokens.remove(token);
          }
        }
        return id;
      } else {
        throw new IllegalStateException();
      }
    } finally {
      lock.unlock();
    }
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
