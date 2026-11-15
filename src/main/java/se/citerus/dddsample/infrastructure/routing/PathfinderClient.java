package se.citerus.dddsample.infrastructure.routing;

import com.pathfinder.api.GraphTraversalService;
import com.pathfinder.api.TransitPath;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Properties;

/**
 * Pathfinder, the routing team's service, called over HTTP (GET /paths) now that it runs as its
 * own application (subject system split). It implements the same GraphTraversalService interface
 * as before, so ExternalRoutingService is unchanged.
 */
public class PathfinderClient implements GraphTraversalService {

  private final RestClient restClient;

  public PathfinderClient(RestClient restClient) {
    this.restClient = restClient;
  }

  @Override
  public List<TransitPath> findShortestPath(String origin, String destination, Properties limitations) {
    return restClient.get()
            .uri(uri -> {
              uri.path("/paths").queryParam("origin", origin).queryParam("destination", destination);
              limitations.stringPropertyNames().forEach(name -> uri.queryParam(name, limitations.getProperty(name)));
              return uri.build();
            })
            .retrieve()
            .body(new ParameterizedTypeReference<List<TransitPath>>() {});
  }
}
