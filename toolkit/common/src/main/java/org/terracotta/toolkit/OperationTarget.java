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

import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.EntityResponse;
import org.terracotta.entity.MessageCodec;
import org.terracotta.entity.MessageCodecException;

/**
 *
 */
public class OperationTarget<S extends EntityMessage, R extends EntityResponse> {
  private final String type;
  private final String name;
  private final MessageCodec<S, R> codec;

  public OperationTarget(String type, String name, MessageCodec<S, R> codec) {
    this.type = type;
    this.name = name;
    this.codec = codec;
  }

  public ToolkitOperation target(S message) throws MessageCodecException {
    return new ToolkitOperation(type, name, codec.encodeMessage(message));
  }
  
  public R process(ToolkitResponse resp) throws MessageCodecException {
    return codec.decodeResponse(resp.payload());
  }
}
