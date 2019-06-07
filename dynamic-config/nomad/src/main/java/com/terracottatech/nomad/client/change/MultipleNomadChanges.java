/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.nomad.client.change;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * Support applying multiple changes at once
 *
 * @author Mathieu Carbou
 */
public class MultipleNomadChanges implements NomadChange {

  private final List<NomadChange> changes;

  @JsonCreator
  public MultipleNomadChanges(@JsonProperty("changes") List<NomadChange> changes) {
    this.changes = new ArrayList<>(requireNonNull(changes));
  }

  public MultipleNomadChanges(NomadChange... changes) {
    this(Arrays.asList(changes));
  }

  public List<NomadChange> getChanges() {
    return changes;
  }

  @Override
  public String getSummary() {
    return changes.stream().map(NomadChange::getSummary).collect(Collectors.joining(", and "));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof MultipleNomadChanges)) return false;
    MultipleNomadChanges that = (MultipleNomadChanges) o;
    return getChanges().equals(that.getChanges());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getChanges());
  }

  @Override
  public String toString() {
    return getSummary();
  }
}
