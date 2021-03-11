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
package org.terracotta.diagnostic.server;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.terracotta.diagnostic.server.api.DiagnosticServicesRegistration;
import org.terracotta.diagnostic.server.api.Expose;
import org.terracotta.json.ObjectMapperFactory;
import org.terracotta.server.ServerMBean;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.mockito.ArgumentMatchers;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.Mockito;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.terracotta.server.Server;
import org.terracotta.server.ServerEnv;
import org.terracotta.server.ServerJMX;
import static org.terracotta.testing.ExceptionMatcher.throwing;

/**
 * @author Mathieu Carbou
 */
public class DefaultDiagnosticServicesRegistrationTest {

  DefaultDiagnosticServices diagnosticServices = new DefaultDiagnosticServices(new ObjectMapperFactory());

  @Before
  public void setUp() throws Exception {
    Server server = mock(Server.class);
    ServerJMX jmx = mock(ServerJMX.class);
    MBeanServer mbean = MBeanServerFactory.newMBeanServer();
    when(server.getManagement()).thenReturn(jmx);
    when(jmx.getMBeanServer()).thenReturn(mbean);
    Mockito.doAnswer(a->{
      ObjectName on = ServerMBean.createMBeanName((String)a.getArgument(0));
      mbean.registerMBean(a.getArgument(1), on);
      return null;
    }).when(jmx).registerMBean(anyString(), ArgumentMatchers.any());
    ServerEnv.setServer(server);
    diagnosticServices.init();
  }

  @After
  public void tearDown() {
    diagnosticServices.close();
  }

  @Test
  public void test_close() throws Throwable {
    assertThat(diagnosticServices.findService(MyService2.class).isPresent(), is(false));

    DiagnosticServicesRegistration<MyService2> registration = diagnosticServices.register(MyService2.class, new MyServiceImpl());

    assertThat(diagnosticServices.findService(MyService2.class).isPresent(), is(true));
    registration.close();

    assertThat(diagnosticServices.findService(MyService2.class).isPresent(), is(false));
    assertThat(
        () -> ServerEnv.getServer().getManagement().getMBeanServer().getMBeanInfo(ServerMBean.createMBeanName("s2")),
        is(throwing(instanceOf(InstanceNotFoundException.class)).andMessage(is(equalTo("org.terracotta:name=s2")))));
    assertThat(
        () -> ServerEnv.getServer().getManagement().getMBeanServer().getMBeanInfo(ServerMBean.createMBeanName("AnotherName")),
        is(throwing(instanceOf(InstanceNotFoundException.class)).andMessage(is(equalTo("org.terracotta:name=AnotherName")))));

    // subsequent init is not failing
    diagnosticServices.register(MyService2.class, new MyServiceImpl()).close();
  }

  public interface MyService2 {
    String say2(String word);
  }

  @Expose("s2")
  public static class MyServiceImpl implements MyService2 {
    @Override
    public String say2(String word) {
      return "2. Hello " + word;
    }
  }

}
