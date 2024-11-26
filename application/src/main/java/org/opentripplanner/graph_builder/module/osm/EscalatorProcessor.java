package org.opentripplanner.graph_builder.module.osm;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.opentripplanner.osm.model.OsmWay;
import org.opentripplanner.street.model.edge.EscalatorEdge;
import org.opentripplanner.street.model.vertex.IntersectionVertex;

/**
 * Contains the logic for extracting escalators out of OSM data
 */
class EscalatorProcessor {

  private final Map<Long, IntersectionVertex> intersectionNodes;

  // If an escalator is tagged as moving less than 5 cm/s, or more than 5 m/s,
  // assume it's an error and ignore it.
  private static final double SLOW_ESCALATOR_ERROR_CUTOFF = 0.05;
  private static final double FAST_ESCALATOR_ERROR_CUTOFF = 5.0;

  public EscalatorProcessor(Map<Long, IntersectionVertex> intersectionNodes) {
    this.intersectionNodes = intersectionNodes;
  }

  public void buildEscalatorEdge(OsmWay escalatorWay, double length) {
    List<Long> nodes = Arrays
      .stream(escalatorWay.getNodeRefs().toArray())
      .filter(nodeRef ->
        intersectionNodes.containsKey(nodeRef) && intersectionNodes.get(nodeRef) != null
      )
      .boxed()
      .toList();

    Optional<Duration> duration = escalatorWay.getDuration();
    if (duration.isPresent()) {
      double speed = length / duration.get().toSeconds();
      if (speed < SLOW_ESCALATOR_ERROR_CUTOFF || speed > FAST_ESCALATOR_ERROR_CUTOFF) {
        duration = Optional.empty();
      }
    }
    for (int i = 0; i < nodes.size() - 1; i++) {
      if (escalatorWay.isForwardEscalator()) {
        EscalatorEdge.createEscalatorEdge(
          intersectionNodes.get(nodes.get(i)),
          intersectionNodes.get(nodes.get(i + 1)),
          length,
          duration.orElse(null)
        );
      } else if (escalatorWay.isBackwardEscalator()) {
        EscalatorEdge.createEscalatorEdge(
          intersectionNodes.get(nodes.get(i + 1)),
          intersectionNodes.get(nodes.get(i)),
          length,
          duration.orElse(null)
        );
      } else {
        EscalatorEdge.createEscalatorEdge(
          intersectionNodes.get(nodes.get(i)),
          intersectionNodes.get(nodes.get(i + 1)),
          length,
          duration.orElse(null)
        );

        EscalatorEdge.createEscalatorEdge(
          intersectionNodes.get(nodes.get(i + 1)),
          intersectionNodes.get(nodes.get(i)),
          length,
          duration.orElse(null)
        );
      }
    }
  }
}
