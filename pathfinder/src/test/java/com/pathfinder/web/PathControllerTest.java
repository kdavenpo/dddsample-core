package com.pathfinder.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pathfinder.api.TransitEdge;
import com.pathfinder.api.TransitPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * GET /paths, the endpoint DDDSample calls. Pathfinder's paths are random, so the test checks
 * their shape: 3 to 5 paths, each from the origin to the destination with connected legs, and
 * JSON that reads back into TransitPath as DDDSample's client reads it.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PathControllerTest {

  @Autowired
  private MockMvc mvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  void returnsConnectedPathsFromOriginToDestination() throws Exception {
    String json = mvc.perform(get("/paths")
                    .param("origin", "CNHKG")
                    .param("destination", "FIHEL")
                    .param("DEADLINE", "2026-12-31T00:00:00Z"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

    List<TransitPath> paths = objectMapper.readValue(json, new TypeReference<List<TransitPath>>() {});
    assertThat(paths).hasSizeBetween(3, 5);
    for (TransitPath path : paths) {
      List<TransitEdge> legs = path.getTransitEdges();
      assertThat(legs).isNotEmpty();
      assertThat(legs.get(0).getFromNode()).isEqualTo("CNHKG");
      assertThat(legs.get(legs.size() - 1).getToNode()).isEqualTo("FIHEL");
      for (int i = 0; i < legs.size() - 1; i++) {
        assertThat(legs.get(i + 1).getFromNode()).isEqualTo(legs.get(i).getToNode());
      }
    }
  }
}
