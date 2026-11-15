package se.citerus.dddsample.infrastructure.routing;

import com.pathfinder.api.GraphTraversalService;
import com.pathfinder.api.TransitEdge;
import com.pathfinder.api.TransitPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import se.citerus.dddsample.domain.model.cargo.*;
import se.citerus.dddsample.domain.model.location.Location;
import se.citerus.dddsample.domain.model.location.LocationRepository;
import se.citerus.dddsample.domain.model.voyage.VoyageNumber;
import se.citerus.dddsample.domain.model.voyage.VoyageRepository;
import se.citerus.dddsample.infrastructure.persistence.inmemory.LocationRepositoryInMem;
import se.citerus.dddsample.infrastructure.sampledata.SampleVoyages;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static se.citerus.dddsample.infrastructure.sampledata.SampleLocations.*;

public class ExternalRoutingServiceTest {

  private ExternalRoutingService externalRoutingService;
  private VoyageRepository voyageRepository;

  @BeforeEach
  public void setUp() {
    LocationRepository locationRepository = new LocationRepositoryInMem();
    voyageRepository = mock(VoyageRepository.class);
    // Pathfinder now runs as its own application, so a fixed path stands in for it:
    // origin -> Tokyo -> Stockholm -> destination, arriving before any deadline.
    GraphTraversalService graphTraversalService = (origin, destination, limitations) -> {
      Instant now = Instant.EPOCH;
      String tokyo = TOKYO.unLocode().idString(), stockholm = STOCKHOLM.unLocode().idString();
      return List.of(new TransitPath(List.of(
              new TransitEdge("0100S", origin, tokyo, now, now),
              new TransitEdge("0100S", tokyo, stockholm, now, now),
              new TransitEdge("0100S", stockholm, destination, now, now))));
    };
    externalRoutingService = new ExternalRoutingService(graphTraversalService, locationRepository, voyageRepository);
  }

  @Test
  public void testCalculatePossibleRoutes() {
    TrackingId trackingId = new TrackingId("ABC");
    RouteSpecification routeSpecification = new RouteSpecification(HONGKONG, HELSINKI, Instant.now());
    Cargo cargo = new Cargo(trackingId, routeSpecification);

    when(voyageRepository.find(isA(VoyageNumber.class))).thenReturn(SampleVoyages.CM002);

    List<Itinerary> candidates = externalRoutingService.fetchRoutesForSpecification(routeSpecification);
    assertThat(candidates).isNotNull();
    assertThat(candidates).hasSize(1);

    for (Itinerary itinerary : candidates) {
      List<Leg> legs = itinerary.legs();
      assertThat(legs).isNotNull();
      assertThat(legs.isEmpty()).isFalse();

      // Cargo origin and start of first leg should match
      assertThat(legs.get(0).loadLocation()).isEqualTo(cargo.origin());

      // Cargo final destination and last leg stop should match
      Location lastLegStop = legs.get(legs.size() - 1).unloadLocation();
      assertThat(lastLegStop).isEqualTo(cargo.routeSpecification().destination());

      for (int i = 0; i < legs.size() - 1; i++) {
        // Assert that all legs are connected
        assertThat(legs.get(i + 1).loadLocation()).isEqualTo(legs.get(i).unloadLocation());
      }
    }
  }
}
