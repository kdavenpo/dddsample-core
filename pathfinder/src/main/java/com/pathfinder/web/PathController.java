package com.pathfinder.web;

import com.pathfinder.api.GraphTraversalService;
import com.pathfinder.api.TransitPath;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * GraphTraversalService.findShortestPath over HTTP:
 * GET /paths?origin=CNHKG&amp;destination=FIHEL&amp;DEADLINE=... ; every parameter other than
 * origin and destination is one of the limitations.
 */
@RestController
public class PathController {

    private final GraphTraversalService graphTraversalService;

    public PathController(GraphTraversalService graphTraversalService) {
        this.graphTraversalService = graphTraversalService;
    }

    @GetMapping("/paths")
    public List<TransitPath> findShortestPath(@RequestParam String origin,
                                              @RequestParam String destination,
                                              @RequestParam Map<String, String> parameters) {
        Properties limitations = new Properties();
        parameters.forEach((name, value) -> {
            if (!name.equals("origin") && !name.equals("destination")) {
                limitations.setProperty(name, value);
            }
        });
        return graphTraversalService.findShortestPath(origin, destination, limitations);
    }
}
