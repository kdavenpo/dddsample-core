package com.pathfinder.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.time.Instant;

/**
 * Represents an edge in a path through a graph,
 * describing the route of a cargo.
 *  
 */
public final class TransitEdge implements Serializable {

  private final String edge;
  private final String fromNode;
  private final String toNode;
  private final Instant fromDate;
  private final Instant toDate;

  /**
   * Constructor.
   *
   * @param edge
   * @param fromNode
   * @param toNode
   * @param fromDate
   * @param toDate
   */
  @JsonCreator   // read back from pathfinder's JSON over HTTP (subject system split)
  public TransitEdge(@JsonProperty("edge") final String edge,
                     @JsonProperty("fromNode") final String fromNode,
                     @JsonProperty("toNode") final String toNode,
                     @JsonProperty("fromDate") final Instant fromDate,
                     @JsonProperty("toDate") final Instant toDate) {
    this.edge = edge;
    this.fromNode = fromNode;
    this.toNode = toNode;
    this.fromDate = fromDate;
    this.toDate = toDate;
  }

  public String getEdge() {
    return edge;
  }

  public String getFromNode() {
    return fromNode;
  }

  public String getToNode() {
    return toNode;
  }

  public Instant getFromDate() {
    return fromDate;
  }

  public Instant getToDate() {
    return toDate;
  }
}