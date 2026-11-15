package com.pathfinder.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 *
 */
public final class TransitPath implements Serializable {

  private final List<TransitEdge> transitEdges;

  /**
   * Constructor.
   *
   * @param transitEdges The legs for this itinerary.
   */
  @JsonCreator   // read back from pathfinder's JSON over HTTP (subject system split)
  public TransitPath(@JsonProperty("transitEdges") final List<TransitEdge> transitEdges) {
    this.transitEdges = transitEdges;
  }

  /**
   * @return An unmodifiable list DTOs.
   */
  public List<TransitEdge> getTransitEdges() {
    return Collections.unmodifiableList(transitEdges);
  }
}
