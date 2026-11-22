package se.citerus.dddsample.split;

import org.springframework.core.env.Environment;

/**
 * Which service this process is in a deployed decomposition (CS 8674, DECISIONS 65), read from
 * the generated split properties:
 *
 *   split.service=1                         this process's service
 *   split.url.0=http://service-0:8080/...   where each other service is reached
 *   split.owner.<class name>=0              the service that owns each class
 *
 * With no split.service, the process is the whole system (the starting system, or the seed step),
 * and every class is local.
 */
public final class Split {

    private final Environment env;

    public Split(Environment env) {
        this.env = env;
    }

    /** This process's service, or null when it runs the whole system. */
    public String me() {
        String me = env.getProperty("split.service");
        return me == null || me.isBlank() ? null : me.trim();
    }

    public boolean active() {
        return me() != null;
    }

    /** True in the seed step, which only creates the schema and loads the sample data, then exits. */
    public boolean seedOnly() {
        return env.getProperty("split.seed-only", Boolean.class, false);
    }

    /** The service that owns a class, or null when no service does (data objects, stateless and shared classes). */
    public String owner(String className) {
        String owner = env.getProperty("split.owner." + className);
        return owner == null || owner.isBlank() ? null : owner.trim();
    }

    /** True when the class runs in this process: always without a split, else when this service owns it. */
    public boolean local(String className) {
        if (!active()) {
            return true;
        }
        String owner = owner(className);
        return owner == null || owner.equals(me());
    }

    /** The base URL of a service, for example http://service-0:8080/dddsample. */
    public String url(String service) {
        String url = env.getProperty("split.url." + service);
        if (url == null || url.isBlank()) {
            throw new IllegalStateException("no split.url." + service + " for this process");
        }
        return url.trim();
    }
}
