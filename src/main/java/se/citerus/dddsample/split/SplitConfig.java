package se.citerus.dddsample.split;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import se.citerus.dddsample.config.RequestIds;

/**
 * Wiring for a deployed decomposition (DECISIONS 65). With split.service set, the beans other
 * services own become HTTP clients and this service answers calls to its own (InternalController); with
 * split.seed-only, the process loads the sample data and exits. With neither, nothing here is
 * created and the application runs as the whole system.
 */
@Configuration
public class SplitConfig {

    @Bean
    public Split split(Environment environment) {
        return new Split(environment);
    }

    @Bean
    @ConditionalOnProperty("split.service")
    public static RemoteBeans remoteBeans(Environment environment) {
        return new RemoteBeans(new Split(environment), RequestIds.passOn());
    }

    @Bean
    @ConditionalOnProperty(name = "split.seed-only", havingValue = "true")
    public CommandLineRunner seedAndExit(ConfigurableApplicationContext context) {
        return args -> System.exit(SpringApplication.exit(context, () -> 0));
    }
}
