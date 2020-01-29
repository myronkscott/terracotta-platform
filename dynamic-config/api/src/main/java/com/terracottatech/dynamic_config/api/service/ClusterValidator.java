/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.api.service;

import com.terracottatech.common.struct.MemoryUnit;
import com.terracottatech.dynamic_config.api.model.Cluster;
import com.terracottatech.dynamic_config.api.model.MalformedClusterException;
import com.terracottatech.dynamic_config.api.model.Node;
import com.terracottatech.dynamic_config.api.model.Setting;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.terracottatech.dynamic_config.api.model.Setting.SECURITY_AUTHC;
import static com.terracottatech.dynamic_config.api.model.Setting.SECURITY_DIR;
import static com.terracottatech.dynamic_config.api.model.Setting.SECURITY_SSL_TLS;
import static com.terracottatech.dynamic_config.api.model.Setting.SECURITY_WHITELIST;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toCollection;

/**
 * This class expects all the fields to be first validated by {@link Setting#validate(String)}.
 * <p>
 * This class will validate the complete cluster object (inter-field checks and dependency checks).
 */
public class ClusterValidator {
  private final Cluster cluster;

  public ClusterValidator(Cluster cluster) {
    this.cluster = cluster;
  }

  public void validate() throws MalformedClusterException {
    validateNodeName();
    validateSecurity();
    validateClientSettings();
    validateServerSettings();
    validateSecurityDir();
  }

  @SuppressWarnings("OptionalGetWithoutIsPresent")
  private void validateNodeName() {
    for (int i = 0; i < cluster.getStripeCount(); i++) {
      int stripeId = i + 1;
      cluster.getStripe(stripeId).get().getNodes()
          .stream()
          .map(Node::getNodeName)
          .filter(Objects::nonNull)
          .collect(groupingBy(identity(), counting()))
          .entrySet()
          .stream()
          .filter(e -> e.getValue() > 1)
          .map(Map.Entry::getKey)
          .findFirst()
          .ifPresent(nodeName -> {
            throw new MalformedClusterException("Found duplicate node name: " + nodeName + " in stripe " + stripeId);
          });
    }
  }

  private void validateSecurity() {
    validate(Node::getSecurityAuthc, "Authentication setting of all nodes should match");
    validate(Node::isSecuritySslTls, "SSL/TLS setting of all nodes should match");
    validate(Node::isSecurityWhitelist, "Whitelist setting of all nodes should match");
  }

  private void validateClientSettings() {
    validate(Node::getClientLeaseDuration, "Client lease duration of all nodes should match");
    validate(Node::getClientReconnectWindow, "Client reconnect window of all nodes should match");
  }

  private void validateServerSettings() {
    validate(
        node -> node.getOffheapResources()
            .entrySet()
            .stream()
            .map(e -> e.getKey() + ":" + e.getValue().to(MemoryUnit.B))
            .collect(toCollection(TreeSet::new)),
        "Offheap resources of all nodes should match");
    validate(node -> node.getDataDirs().keySet(), "Data directory names of all nodes should match");
    validate(Node::getFailoverPriority, "Failover setting of all nodes should match");
  }

  private void validateSecurityDir() {
    cluster.nodeContexts().forEach(nodeContext -> {
      Node node = nodeContext.getNode();
      if ("certificate".equals(node.getSecurityAuthc()) && !node.isSecuritySslTls()) {
        throw new MalformedClusterException(SECURITY_SSL_TLS + " is required for " + SECURITY_AUTHC + "=certificate");
      }
      if ((node.getSecurityAuthc() != null && node.getSecurityDir() == null)
          || (node.getSecurityAuditLogDir() != null && node.getSecurityDir() == null)
          || (node.isSecuritySslTls() && node.getSecurityDir() == null)
          || (node.isSecurityWhitelist() && node.getSecurityDir() == null)) {
        throw new MalformedClusterException(SECURITY_DIR + " is mandatory for any of the security configuration");
      }
      if (node.getSecurityDir() != null && !node.isSecuritySslTls() && node.getSecurityAuthc() == null && !node.isSecurityWhitelist()) {
        throw new MalformedClusterException("One of " + SECURITY_SSL_TLS + ", " + SECURITY_AUTHC + ", or " + SECURITY_WHITELIST + " is required for security configuration");
      }
    });
  }

  private void validate(Function<? super Node, Object> function, String errorMsg) {
    Collection<Object> settings = cluster.getNodes().stream()
        .map(function)
        .collect(Collectors.toSet());
    if (settings.size() > 1) { // 0 means no node has the setting, 1 means all nodes have the same setting
      throw new MalformedClusterException(errorMsg + ", but found the following mismatches: " + settings);
    }
  }
}
