package org.terracotta.toolkit;

import com.tc.util.Assert;
import org.terracotta.connection.Connection;
import org.terracotta.connection.entity.EntityRef;
import org.terracotta.passthrough.IClientTestEnvironment;
import org.terracotta.passthrough.IClusterControl;
import org.terracotta.passthrough.ICommonTest;

import java.net.URI;
import java.util.Properties;
import org.terracotta.connection.ConnectionException;
import org.terracotta.connection.ConnectionFactory;
import org.terracotta.toolkit.barrier.Barrier;
import org.terracotta.toolkit.barrier.BarrierConfig;


public class ToolkitTest implements ICommonTest {
  @Override
  public void runSetup(IClientTestEnvironment env, IClusterControl control) throws Throwable {

  }
  
  @Override
  public void runDestroy(IClientTestEnvironment env, IClusterControl control) throws Throwable {

  }
  
  @SuppressWarnings("resource")
  @Override
  public void runTest(IClientTestEnvironment env, IClusterControl control) throws Throwable {
    URI uri = URI.create(env.getClusterUri());
    Connection connection = null;
    try {
      Properties emptyProperties = new Properties();
      connection = ConnectionFactory.connect(uri, emptyProperties);
    } catch (ConnectionException e) {
      org.terracotta.testing.common.Assert.unexpected(e);
    }
    EntityRef<Toolkit, ToolkitConfig> ref = connection.getEntityRef(Toolkit.class, Toolkit.VERSION, ToolkitConstants.STANDARD_TOOLKIT);
    Toolkit toolkit = ref.fetchEntity();
    Barrier b = toolkit.createBarrier("my-barrier", new BarrierConfig(2));
    if (b == null) {
      b = toolkit.getBarrier("my-barrier");
    }
    Assert.assertNotNull(b);
    System.out.println("parties:" + b.getParties());
    int id = b.await();
    System.out.println("id:" + id);
    connection.close();
  }
}