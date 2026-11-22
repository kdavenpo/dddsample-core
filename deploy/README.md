# Deploying a decomposition (CS 8674, DECISIONS 65)

This branch (`c3-deployment`) runs Cargo Tracking's C3 as separate services, and the starting system
(C0) in the same setup as the baseline. The analysis lives in `kdavenpo/cs8674-project`.

## How the split works

Every service runs the same jar (this branch) with its own `split-service-<n>.properties`, generated
from the decomposition by `python -m deploy.generate` in cs8674-project. At startup:

- each bean whose class another service owns is replaced by an HTTP client of the same type
  (`split/RemoteBeans`, `split/RemoteCall`): a call to it is `POST /internal/<Class>/<method>`
  with the arguments as a JSON array (`split/SplitJson`), answered by the owner
  (`split/InternalController`);
- queue listeners and the upload scanner run only in the service that owns them
  (`split/ConditionalOnOwner`);
- data objects, stateless and shared classes are in every service, so calls to them stay local;
- when the service owns pathfinder's route finder, it runs in-process (`split/LocalPathfinder`).

So the calls between services are exactly the build specification's (cs8674-project,
`docs/build_spec_cargo_C3.md`), each over HTTP. Changes to the starting system's own code: the
booking facade gets its itinerary assembler injected instead of creating it (choice C); the
sample-data generator can be switched off (`dddsample.sample-data`); the pom compiles in
pathfinder's `internal` package and adds Jackson's Hibernate module.

## Containers

| Container | C0 (`c0/`) | C3 (`c3/`, generated) |
|---|---|---|
| `db` | HSQLDB server, in memory | the same |
| `activemq` | ActiveMQ Classic 6.1.2 | the same |
| `seed` | (cargo-core loads its own data) | creates the schema, loads the sample data, exits |
| application | `cargo-core`, `pathfinder` (branch subject-system) | `service-0` … `service-4` (this branch) |
| `router` | nginx: everything to cargo-core | nginx: each endpoint to its service |

Every container has 1 CPU and 768 MB. The router listens on port 8080, where the load test points.
Access logs go to `deploy/runs/<RUN>/`.

## Build and run (PowerShell, from the repository root)

```
.\deploy\build.ps1                                  # jars and images
cd deploy\c0;  $env:RUN="c0-run1"; docker compose up -d     # or deploy\c3
docker compose logs -f                              # until every service has started
docker compose down                                 # after the run; the database is in memory
```
