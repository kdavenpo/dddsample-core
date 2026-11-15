package com.pathfinder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Pathfinder as its own application. Its wiring (config.PathfinderApplicationContext) and its
 * implementation (internal) are unchanged; only the call from DDDSample now arrives over HTTP.
 */
@SpringBootApplication
public class PathfinderApplication {

    public static void main(String[] args) {
        SpringApplication.run(PathfinderApplication.class, args);
    }
}
