/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.cli.service.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.converters.PathConverter;
import com.terracottatech.diagnostic.client.connection.DiagnosticServices;
import com.terracottatech.dynamic_config.cli.common.InetSocketAddressConverter;
import com.terracottatech.dynamic_config.cli.common.Usage;
import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.dynamic_config.model.NodeContext;
import com.terracottatech.dynamic_config.model.config.ConfigFileParser;
import com.terracottatech.dynamic_config.model.validation.ConfigFileFormatValidator;
import com.terracottatech.dynamic_config.nomad.ClusterActivationNomadChange;
import com.terracottatech.dynamic_config.util.PropertiesFileLoader;
import com.terracottatech.nomad.client.results.NomadFailureRecorder;
import com.terracottatech.utilities.Tuple2;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static com.terracottatech.dynamic_config.util.IParameterSubstitutor.identity;
import static com.terracottatech.utilities.Assertion.assertNonNull;

@Parameters(commandNames = "activate", commandDescription = "Activate the cluster")
@Usage("activate <-s HOST[:PORT] | -f CONFIG-PROPERTIES-FILE> -n CLUSTER-NAME -l LICENSE-FILE")
public class ActivateCommand extends RemoteCommand {

  @Parameter(names = {"-s"}, description = "Node to connect to", converter = InetSocketAddressConverter.class)
  private InetSocketAddress node;

  @Parameter(names = {"-f"}, description = "Config properties file", converter = PathConverter.class)
  private Path configPropertiesFile;

  @Parameter(names = {"-n"}, description = "Cluster name")
  private String clusterName;

  @Parameter(required = true, names = {"-l"}, description = "Path to license file", converter = PathConverter.class)
  private Path licenseFile;

  private ConfigFileParser configFileParser;
  private Cluster cluster;

  @Override
  public void validate() {
    if (node == null && configPropertiesFile == null) {
      throw new IllegalArgumentException("One of node or config properties file must be specified");
    }

    if (node != null && configPropertiesFile != null) {
      throw new IllegalArgumentException("Either node or config properties file should be specified, not both");
    }

    if (node != null && clusterName == null) {
      throw new IllegalArgumentException("Cluster name should be provided when node is specified");
    }

    if (configPropertiesFile != null) {
      Properties properties = new PropertiesFileLoader(configPropertiesFile).loadProperties();
      ConfigFileFormatValidator configFileValidator = new ConfigFileFormatValidator(configPropertiesFile, properties);
      configFileValidator.validate();
      configFileParser = new ConfigFileParser(configPropertiesFile, properties, identity());
      if (clusterName == null) {
        clusterName = configFileParser.getClusterName();
      }
    }

    assertNonNull(licenseFile, "licenseFile must not be null");
    assertNonNull(clusterName, "clusterName must not be null");
    assertNonNull(multiDiagnosticServiceProvider, "connectionFactory must not be null");
    assertNonNull(nomadManager, "nomadManager must not be null");
    assertNonNull(restartService, "restartService must not be null");
    logger.debug("Command validation successful");
  }

  @Override
  public final void run() {
    cluster = loadCluster();

    cluster.setName(clusterName);
    logger.debug("Setting cluster name successful");

    boolean isClusterActive = validateActivationState(cluster.getNodeAddresses());
    if (isClusterActive) {
      throw new IllegalStateException("Cluster is already activated: " + toString(cluster.getNodeAddresses()));
    }

    try (DiagnosticServices connection = multiDiagnosticServiceProvider.fetchDiagnosticServices(cluster.getNodeAddresses())) {
      prepareActivation(connection);
      logger.info("License installation successful");
      logger.debug("Setting nomad writable successful");

      runNomadChange();
      logger.debug("Nomad change run successful");

      restartNodes(cluster.getNodeAddresses());
    }

    logger.info("Command successful!\n");
  }

  /*<-- Test methods --> */
  Cluster getCluster() {
    return cluster;
  }

  ActivateCommand setNode(InetSocketAddress node) {
    this.node = node;
    return this;
  }

  ActivateCommand setConfigPropertiesFile(Path configPropertiesFile) {
    this.configPropertiesFile = configPropertiesFile;
    return this;
  }

  ActivateCommand setClusterName(String clusterName) {
    this.clusterName = clusterName;
    return this;
  }

  ActivateCommand setLicenseFile(Path licenseFile) {
    this.licenseFile = licenseFile;
    return this;
  }

  String getClusterName() {
    return clusterName;
  }

  private Cluster loadCluster() {
    Cluster cluster;
    if (node != null) {
      cluster = getRemoteTopology(node);
      logger.debug("Cluster topology validation successful");
    } else {
      cluster = configFileParser.createCluster();
      logger.debug("Config property file parsing and cluster topology validation successful");
    }
    return cluster;
  }

  private void prepareActivation(DiagnosticServices connection) {
    logger.debug("Contacting all cluster nodes: {} to prepare for activation", toString(cluster.getNodeAddresses()));
    topologyServices(connection).map(Tuple2::getT2).forEach(ts -> ts.prepareActivation(cluster, read(licenseFile)));
  }

  private void runNomadChange() {
    NomadFailureRecorder<NodeContext> failures = new NomadFailureRecorder<>();
    nomadManager.runChange(cluster.getNodeAddresses(), new ClusterActivationNomadChange(cluster), failures);
    failures.reThrow();
  }


  private static String read(Path path) {
    try {
      return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
