package org.terracotta.toolkit;

import java.util.List;
import java.util.Vector;
import org.terracotta.testing.api.BasicTestClusterConfiguration;

import org.terracotta.testing.demos.TestHelpers;


public class ToolkitGalvanTest extends KitTestBasic {
  @Override
  public String getConfigNamespaceSnippet() {
    return "";
  }

  @Override
  public List<String> getTestJarPaths() {
    Vector<String> jars = new Vector<>();
    jars.add(TestHelpers.jarContainingClass(TerracottaToolkitEntityServerService.class));
    return jars;
  }

  @Override
  public String getServiceConfigXMLSnippet() {
    return "";
  }

  @Override
  public String getEntityConfigXMLSnippet() {
    return "";
  }

  @Override
  public String getTestClassName() {
    return ToolkitTest.class.getName();
  }

  @Override
  public int getClientsToStart() {
    return 2;
  }

  @Override
  public boolean isRestartable() {
    return true;
  }

  @Override
  public List<BasicTestClusterConfiguration> getRunConfigurations() {
    Vector<BasicTestClusterConfiguration> configurationList = new Vector<>();
    configurationList.add(new BasicTestClusterConfiguration("OneActive", 1));
    return configurationList;
  }
}
