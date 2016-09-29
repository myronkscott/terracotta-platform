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

import java.nio.ByteBuffer;
import org.terracotta.runnel.Struct;
import org.terracotta.runnel.StructBuilder;

/**
 *
 */
public class BarrierConfig {
  private final int parties;
  
  private static Struct CONFIG = StructBuilder.newStructBuilder().int32("parties", 1).build();

  public BarrierConfig(byte[] raw) {
    parties = CONFIG.decoder(ByteBuffer.wrap(raw)).int32("parties");
  }
  
  public BarrierConfig(int parties) {
    this.parties = parties;
  }
  
  public int parties() {
    return parties;
  }
  
  public byte[] toRaw() {
    ByteBuffer buffer = CONFIG.encoder().int32("parties", parties).encode();
    byte[] raw = new byte[buffer.flip().remaining()];
    buffer.get(raw);
    return raw;
  }
}
