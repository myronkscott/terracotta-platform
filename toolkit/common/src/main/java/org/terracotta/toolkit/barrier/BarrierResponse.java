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

import org.terracotta.entity.EntityResponse;

/**
 *
 */
public class BarrierResponse implements EntityResponse {
  private final long generation;
  private final int waitCount;
  public final boolean generationOnly;

  public BarrierResponse(boolean generationOnly, long generation, int waitCount) {
    this.generationOnly = generationOnly;
    this.generation = generation;
    this.waitCount = waitCount;
  }

  public long getGeneration() {
    return generation;
  }

  public int getWaitCount() {
    return waitCount;
  }
  
  public boolean isGenerationOnly() {
    return generationOnly;
  }
}
