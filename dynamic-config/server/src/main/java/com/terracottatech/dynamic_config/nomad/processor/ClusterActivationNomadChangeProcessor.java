/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.nomad.processor;

import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.dynamic_config.model.NodeContext;
import com.terracottatech.dynamic_config.nomad.ClusterActivationNomadChange;
import com.terracottatech.nomad.server.NomadException;

import static java.util.Objects.requireNonNull;

public class ClusterActivationNomadChangeProcessor implements NomadChangeProcessor<ClusterActivationNomadChange> {
  private final int stripeId;
  private final String nodeName;
  private final Cluster expectedCluster;

  public ClusterActivationNomadChangeProcessor(int stripeId, String nodeName, Cluster expectedCluster) {
    this.stripeId = stripeId;
    this.nodeName = requireNonNull(nodeName);
    this.expectedCluster = expectedCluster.clone();
  }

  @Override
  public NodeContext tryApply(NodeContext baseConfig, ClusterActivationNomadChange change) throws NomadException {
    if (baseConfig != null) {
      throw new NomadException("Existing config must be null. Found: " + baseConfig);
    }

    // This check verifies that TopologyService#prepareActivation() has been previously called before running any Nomad change
    // to install the topology and license on the node
    if (!expectedCluster.equals(change.getCluster())) {
      throw new NomadException("Wrong cluster: got " + change.getCluster() + ", but was expecting: " + expectedCluster);
    }

    return new NodeContext(change.getCluster(), stripeId, nodeName);
  }

  @Override
  public void apply(ClusterActivationNomadChange change) {
    // no-op
  }
}
