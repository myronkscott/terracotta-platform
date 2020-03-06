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
package org.terracotta.dynamic_config.cli.config_tool.command;

import com.beust.jcommander.Parameters;
import org.terracotta.common.struct.TimeUnit;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Stripe;
import org.terracotta.dynamic_config.api.model.nomad.NodeNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.NodeRemovalNomadChange;
import org.terracotta.dynamic_config.cli.command.Usage;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Collection;

import static java.util.Collections.singletonList;
import static org.terracotta.dynamic_config.cli.config_tool.converter.OperationType.NODE;

/**
 * @author Mathieu Carbou
 */
@Parameters(commandNames = "detach", commandDescription = "Detach a node from a stripe, or a stripe from a cluster")
@Usage("detach [-t node|stripe] -d <hostname[:port]> -s <hostname[:port]> [-f] [-W <restart-wait-time>] [-D <restart-delay>]")
public class DetachCommand extends TopologyCommand {

  @Override
  public void validate() {
    super.validate();

    Collection<InetSocketAddress> destinationPeers = destinationCluster.getNodeAddresses();

    if (destinationPeers.size() == 1) {
      throw new IllegalStateException("Unable to detach since destination cluster contains only 1 node");
    }

    if (!destinationCluster.containsNode(source)) {
      throw new IllegalStateException("Source node: " + source + " is not part of cluster at: " + destination);
    }
  }

  @Override
  protected Cluster updateTopology() {
    Cluster cluster = destinationCluster.clone();

    switch (operationType) {

      case NODE: {
        logger.info("Detaching node: {} from stripe: {}", source, destination);
        cluster.detachNode(source);
        break;
      }

      case STRIPE: {
        Stripe stripe = cluster.getStripe(source).get();
        logger.info("Detaching stripe containing nodes: {} from cluster: {}", toString(stripe.getNodeAddresses()), destination);
        cluster.detachStripe(stripe);
        break;
      }

      default: {
        throw new UnsupportedOperationException(operationType.name());
      }
    }

    return cluster;
  }

  @Override
  protected NodeNomadChange buildNomadChange(Cluster result) {
    switch (operationType) {
      case NODE:
        return new NodeRemovalNomadChange(result, sourceCluster.getStripeId(source).getAsInt(), sourceCluster.getNode(source).get());
      case STRIPE: {
        throw new UnsupportedOperationException("Topology modifications of whole stripes on an activated cluster is not yet supported");
      }
      default: {
        throw new UnsupportedOperationException(operationType.name());
      }
    }
  }

  @Override
  protected void afterNomadChange(Cluster result) {
    Collection<InetSocketAddress> removedNodes = operationType == NODE ?
        singletonList(source) :
        sourceCluster.getStripe(source).get().getNodeAddresses();

    logger.info("Restarting nodes: {}", toString(removedNodes));
    restartNodes(
        removedNodes,
        Duration.ofMillis(restartWaitTime.getQuantity(TimeUnit.MILLISECONDS)),
        Duration.ofMillis(restartDelay.getQuantity(TimeUnit.MILLISECONDS)));
    logger.info("All nodes came back up");
  }
}