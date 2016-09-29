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

import org.terracotta.toolkit.ConcurrencyHelper;

/**
 *
 */
public class BarrierConcurrency implements ConcurrencyHelper {

  @Override
  public int concurrencyKey(String name, byte[] m, int min, int max) {
//  everyting is serialized, just hash the name
    int val = name.hashCode() % max;
    if (val < min) {
      val = min;
    }
    return val;
  }
  
}
