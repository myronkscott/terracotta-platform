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
import java.util.UUID;
import org.terracotta.entity.MessageCodec;
import org.terracotta.entity.MessageCodecException;
import org.terracotta.runnel.Struct;
import org.terracotta.runnel.StructBuilder;
import org.terracotta.runnel.decoding.ArrayDecoder;
import org.terracotta.runnel.decoding.StructDecoder;

/**
 *
 */
public class BarrierCodec implements MessageCodec<BarrierRequest, BarrierResponse> {

  private static final Struct BARRIER_STRUCT = StructBuilder.newStructBuilder()
          .int64s("uuid", 1)
          .build();    
  
  private static final Struct BARRIER_RESPONSE = StructBuilder.newStructBuilder()
          .int32("generationOnly", 1)
          .int64("generation", 2)
          .int32("waitCount", 3)
          .build(); 
    
  public BarrierCodec() {
  }

  @Override
  public byte[] encodeMessage(BarrierRequest m) throws MessageCodecException {
    ByteBuffer buffer = BARRIER_STRUCT.encoder()
            .int64s("uuid")
            .value(m.getUuid().getLeastSignificantBits())
            .value(m.getUuid().getMostSignificantBits()).end().encode();
    byte[] data = new byte[buffer.flip().remaining()];
    buffer.get(data);
    return data;
  }

  @Override
  public BarrierRequest decodeMessage(byte[] bytes) throws MessageCodecException {
    ArrayDecoder<Long> bits = BARRIER_STRUCT.decoder(ByteBuffer.wrap(bytes)).int64s("uuid");
    return new BarrierRequest(new UUID(bits.value(), bits.value()));
  }

  @Override
  public byte[] encodeResponse(BarrierResponse r) throws MessageCodecException {
    ByteBuffer buffer = BARRIER_RESPONSE.encoder()
            .int32("generationOnly", r.isGenerationOnly()?1:0)
            .int64("generation", r.getGeneration())
            .int32("waitCount", r.getWaitCount())
            .encode();
    byte[] data = new byte[buffer.flip().remaining()];
    buffer.get(data);
    return data;
  }

  @Override
  public BarrierResponse decodeResponse(byte[] bytes) throws MessageCodecException {
    StructDecoder decode = BARRIER_RESPONSE.decoder(ByteBuffer.wrap(bytes));
    return new BarrierResponse(decode.int32("generationOnly") == 1, decode.int64("generation"), decode.int32("waitCount"));
  }
}
