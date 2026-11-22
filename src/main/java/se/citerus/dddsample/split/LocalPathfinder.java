package se.citerus.dddsample.split;

import com.pathfinder.api.GraphTraversalService;
import com.pathfinder.internal.GraphDAOStub;
import com.pathfinder.internal.GraphTraversalServiceImpl;

/**
 * Pathfinder's route finder, in-process, for the service that owns it: when a decomposition puts
 * cargo's routing code and pathfinder in one service, the call between them is local, as the
 * remote-call count assumes. Pathfinder's sources are compiled into this build (pom.xml).
 */
public final class LocalPathfinder {

    private LocalPathfinder() {
    }

    public static GraphTraversalService create() {
        return new GraphTraversalServiceImpl(new GraphDAOStub());
    }
}
