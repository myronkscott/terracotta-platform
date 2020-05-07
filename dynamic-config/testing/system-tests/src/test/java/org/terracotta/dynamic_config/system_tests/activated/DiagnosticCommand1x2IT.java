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
package org.terracotta.dynamic_config.system_tests.activated;

import org.junit.Rule;
import org.junit.Test;
import org.terracotta.angela.client.support.junit.NodeOutputRule;
import org.terracotta.dynamic_config.test_support.ClusterDefinition;
import org.terracotta.dynamic_config.test_support.DynamicConfigIT;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertThat;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.containsLinesInOrderStartingWith;

/**
 * @author Mathieu Carbou
 */
@ClusterDefinition(nodesPerStripe = 2, autoStart = false)
public class DiagnosticCommand1x2IT extends DynamicConfigIT {

  @Rule public final NodeOutputRule out = new NodeOutputRule();

  @Test
  public void test_diagnostic_on_unconfigured_node() throws Exception {
    startNode(1, 1);
    waitForDiagnostic(1, 1);
    assertThat(configToolInvocation("diagnostic", "-s", "localhost:" + getNodePort(1, 1)),
        containsLinesInOrderStartingWith(Files.lines(Paths.get(getClass().getResource("/diagnostic1.txt").toURI())).collect(toList())));
  }

  @Test
  public void test_diagnostic_on_activated_node() throws Exception {
    startNode(1, 1);
    waitForDiagnostic(1, 1);
    activateCluster();
    assertThat(configToolInvocation("diagnostic", "-s", "localhost:" + getNodePort(1, 1)),
        containsLinesInOrderStartingWith(Files.lines(Paths.get(getClass().getResource("/diagnostic2.txt").toURI())).collect(toList())));
  }

  @Test
  public void test_diagnostic_on_repair_mode() throws Exception {
    startNode(1, 1);
    waitForDiagnostic(1, 1);
    activateCluster();

    String nodeName = getNode(1, 1).getServerSymbolicName().getSymbolicName();
    String repo = getNode(1, 1).getConfigRepo();

    stopNode(1, 1);

    startNode(1, 1, "--repair-mode", "--name", nodeName, "-r", repo);
    assertThat(configToolInvocation("diagnostic", "-s", "localhost:" + getNodePort(1, 1)),
        containsLinesInOrderStartingWith(Files.lines(Paths.get(getClass().getResource("/diagnostic3.txt").toURI())).collect(toList())));
  }

  @Test
  public void test_diagnostic_on_cluster_with_activated_and_diagnostic_node() throws Exception {
    // NOTE: this situation can happen when:
    // - starting a stripe config pre-configured, and then starting other nodes (the first active node started would contain a topology where other nodes could still be unconfigured)
    // - failover during nomad commit when detaching a node: upon restart, the node will be PREPARED, will have a topology pointing to a node that has been restarted in diagnostic mode

    Path configurationFile = copyConfigProperty("/config-property-files/1x2.properties");
    startNode(1, 1, "--auto-activate", "-f", configurationFile.toString(), "-s", "localhost", "-p", String.valueOf(getNodePort(1, 1)), "--config-dir", "config/stripe1/node-1-1");
    waitForActive(1, 1);

    startNode(1, 2);
    waitForDiagnostic(1, 2);

    assertThat(configToolInvocation("diagnostic", "-s", "localhost:" + getNodePort(1, 1)),
        containsLinesInOrderStartingWith(Files.lines(Paths.get(getClass().getResource("/diagnostic4.txt").toURI())).collect(toList())));
  }

}
