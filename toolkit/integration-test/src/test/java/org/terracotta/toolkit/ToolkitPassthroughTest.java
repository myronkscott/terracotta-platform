package org.terracotta.toolkit;

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
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.terracotta.connection.entity.EntityRef;
import org.terracotta.passthrough.PassthroughConnection;
import org.terracotta.passthrough.PassthroughServer;


import org.terracotta.toolkit.TerracottaToolkitEntityClientService;
import org.terracotta.toolkit.TerracottaToolkitEntityServerService;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.ToolkitConfig;
import org.terracotta.toolkit.ToolkitConstants;
import org.terracotta.toolkit.barrier.Barrier;
import org.terracotta.toolkit.barrier.BarrierConfig;

/**
 */
public class ToolkitPassthroughTest {

  private PassthroughServer server;

  @Before
  public void setUp() throws Exception {
    server = new PassthroughServer();
    server.registerClientEntityService(new TerracottaToolkitEntityClientService());
    server.registerServerEntityService(new TerracottaToolkitEntityServerService());
    boolean isActive = true;
    boolean shouldLoadStorage = false;
    server.start(isActive, shouldLoadStorage);
    PassthroughConnection connection = server.connectNewClient("connectionName");
    EntityRef<Toolkit, ToolkitConfig> entityRef = connection.getEntityRef(Toolkit.class, Toolkit.VERSION, ToolkitConstants.STANDARD_TOOLKIT);
    entityRef.create(new ToolkitConfig());
  }

  @After
  public void tearDown() {
    server.stop();
  }

  @Test
  public void testWithBarrierType() throws Exception {
    PassthroughConnection connection = server.connectNewClient("connectionName");
    EntityRef<Toolkit, Object> entityRef = connection.getEntityRef(Toolkit.class, Toolkit.VERSION, ToolkitConstants.STANDARD_TOOLKIT);
    Toolkit toolkit = entityRef.fetchEntity();
    Barrier b = toolkit.createBarrier("my-barrier", new BarrierConfig());
    Assert.assertNotNull(b);
    b.close();
    try {
      b.await();
      Assert.fail();
    } catch (Exception exp) {
      // expected;
    }
    b = toolkit.createBarrier("my-barrier", new BarrierConfig());
    Assert.assertNotNull(b);
  }
}
