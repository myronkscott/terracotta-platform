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


import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import org.terracotta.entity.MessageCodec;
import org.terracotta.entity.MessageCodecException;
import org.terracotta.runnel.EnumMapping;
import org.terracotta.runnel.EnumMappingBuilder;
import org.terracotta.runnel.Struct;
import org.terracotta.runnel.StructBuilder;
import org.terracotta.runnel.decoding.StructArrayDecoder;
import org.terracotta.runnel.decoding.StructDecoder;
import org.terracotta.runnel.encoding.StructArrayEncoder;


public class ToolkitCodec implements MessageCodec<ToolkitMessage, ToolkitResponse> {

  private static final EnumMapping TOOLKIT_CMDS = EnumMappingBuilder.newEnumMappingBuilder(ToolkitCommand.class)
          .mapping(ToolkitCommand.CREATE, 1)
          .mapping(ToolkitCommand.GET, 2)
          .mapping(ToolkitCommand.RELEASE, 3)
          .mapping(ToolkitCommand.OPERATION, 4).build();

  private static final Struct TOOLKIT_STRUCT = StructBuilder.newStructBuilder()
          .enm("cmd", 1, TOOLKIT_CMDS)
          .string("type", 2)
          .string("name", 3)
          .byteBuffer("payload", 4)
          .build();  
  
  private static final EnumMapping TOOLKIT_RESULTS = EnumMappingBuilder.newEnumMappingBuilder(ToolkitResult.class)
          .mapping(ToolkitResult.SUCCESS, 1)
          .mapping(ToolkitResult.FAIL, 2).build();
  
  private static final Struct TOOLKIT_RESPONSE = StructBuilder.newStructBuilder()
          .string("type", 1)
          .string("name", 2)
          .enm("result", 3, TOOLKIT_RESULTS)
          .byteBuffer("payload", 4)
          .build();

  private static final Struct RECONNECT_DATA = StructBuilder.newStructBuilder()
          .string("type", 1)
          .string("name", 2)
          .string("payload", 3).build();
  
  private static final Struct RECONNECT_COLLECTION = StructBuilder.newStructBuilder()
          .structs("targets", 1, RECONNECT_DATA).build();

  
  @Override
  public byte[] encodeMessage(ToolkitMessage m) throws MessageCodecException {
    ByteBuffer buffer = TOOLKIT_STRUCT.encoder().enm("cmd", m.command())
            .string("type", m.type())
            .string("name", m.name())
            .byteBuffer("payload", ByteBuffer.wrap(m.payload())).encode();
    byte[] data = new byte[buffer.flip().remaining()];
    buffer.get(data);
    return data;
  }

  @Override
  public ToolkitMessage decodeMessage(byte[] bytes) throws MessageCodecException {
    StructDecoder decode = TOOLKIT_STRUCT.decoder(ByteBuffer.wrap(bytes));
    ToolkitCommand cmd = decode.enm("cmd");
    String type = decode.string("type");
    String name = decode.string("name");
    ByteBuffer payload = decode.byteBuffer("payload");
    return translate(cmd, type, name, payload);
  }
  
  private static ToolkitMessage translate(ToolkitCommand cmd, String type, String name, final ByteBuffer payload) {
    switch (cmd) {
      case GET:
        return new GetToolkitObject(type, name);
      case CREATE: {
        byte[] data = new byte[payload.remaining()];
        payload.get(data);
        return new CreateToolkitObject(type, name, data);
      }
      case RELEASE:
        return new ReleaseToolkitObject(type, name);
      case OPERATION: {
        byte[] data = new byte[payload.remaining()];
        payload.get(data);
        return new ToolkitOperation(type, name, data);
      }
      default:
        throw new IllegalArgumentException(cmd + " " + type + " " + name);
    }
  }

  @Override
  public byte[] encodeResponse(ToolkitResponse r) throws MessageCodecException {
    ByteBuffer buf = TOOLKIT_RESPONSE.encoder()
            .enm("result", r.result())
            .encode();
    byte[] send = new byte[buf.flip().remaining()];
    buf.get(send);
    return send;
  }

  @Override
  public ToolkitResponse decodeResponse(final byte[] bytes) throws MessageCodecException {
    final ByteBuffer buffer = ByteBuffer.wrap(bytes);
    return new ToolkitResponse() {
      @Override
      public ToolkitResult result() {
        return TOOLKIT_RESPONSE.decoder(buffer).enm("result");
      }

      @Override
      public byte[] payload() {
        return TOOLKIT_RESPONSE.decoder(buffer).enm("payload");
      }

      @Override
      public String type() {
        return TOOLKIT_RESPONSE.decoder(buffer).enm("type");
      }

      @Override
      public String name() {
        return TOOLKIT_RESPONSE.decoder(buffer).enm("name");
      }
    };
  }
  
  public static byte[] encodeReconnectData(Collection<ToolkitReconnectData> data) {
    StructArrayEncoder collection = RECONNECT_COLLECTION.encoder()
      .structs("targets");
    Iterator<ToolkitReconnectData> items = data.iterator();
    while (items.hasNext()) {
      ToolkitReconnectData in = items.next();
      collection.next();
      collection.string("type", in.getType())
              .string("name",in.getName())
              .byteBuffer("payload", ByteBuffer.wrap(in.getPayload()));
//      if (items.hasNext()) {
//        collection.next();
//      }
    }
    ByteBuffer buffer = collection.end().encode();
    byte[] send = new byte[buffer.flip().remaining()];
    buffer.get(send);
    return send;
  }
  
  public Collection<ToolkitReconnectData> decodeReconnectData(byte[] data) {
    StructArrayDecoder collection = RECONNECT_COLLECTION.decoder(ByteBuffer.wrap(data)).structs("targets");
    int size = collection.length();
    ToolkitReconnectData[] vals = new ToolkitReconnectData[size];
    for (int x=0;x<size;x++) {
      collection.next();
      ByteBuffer pay = collection.byteBuffer("payload");
      byte[] payload = new byte[pay.remaining()];
      pay.get(payload);
      vals[0] = new ToolkitReconnectData(collection.string("type"), collection.string("name"), payload);
//      if (x < size - 1) {
//        collection.next();
//      }
    }
    return Arrays.asList(vals);
  }
}
