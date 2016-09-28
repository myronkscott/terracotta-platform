package org.terracotta.toolkit;

import java.util.ArrayList;
import java.util.List;

import org.terracotta.testing.api.BasicTestClusterConfiguration;
import org.terracotta.testing.api.ITestMaster;
import org.terracotta.testing.demos.TestHelpers;
import org.terracotta.testing.support.BasicHarnessTest;


public abstract class KitTestBasic extends BasicHarnessTest implements ITestMaster<BasicTestClusterConfiguration> {
  @Override
  public ITestMaster<BasicTestClusterConfiguration> getTestMaster() {
    return this;
  }

  @Override
  public String getClientErrorHandlerClassName() {
    // Use the integration error handler.
    return null;
  }

  @Override
  public final List<String> getExtraServerJarPaths() {
    List<String> jarPaths = new ArrayList<>(getTestJarPaths());
    return jarPaths;
  }

  protected abstract List<String> getTestJarPaths();
}
