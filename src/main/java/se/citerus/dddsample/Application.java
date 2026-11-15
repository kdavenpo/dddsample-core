package se.citerus.dddsample;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import se.citerus.dddsample.config.DDDSampleApplicationContext;

@Import(DDDSampleApplicationContext.class)   // Pathfinder now runs as its own application (see pathfinder/)
@SpringBootApplication
public class Application {

    public static void main(String[] args) throws Exception {
        SpringApplication.run(Application.class, args);
    }
}