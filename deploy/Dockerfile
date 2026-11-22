# One image per jar (CS 8674, DECISIONS 65). JAR is a file in deploy/jars/.
# The HSQLDB server jar is taken out of the application jar, so the database container runs the
# same HSQLDB version the application was built with.
FROM eclipse-temurin:17.0.13_11-jdk AS libs
ARG JAR
COPY jars/${JAR} /app/app.jar
RUN mkdir -p /x && cd /x && jar xf /app/app.jar BOOT-INF/lib \
 && (cp BOOT-INF/lib/hsqldb-*.jar /app/hsqldb.jar 2>/dev/null || true)

FROM eclipse-temurin:17.0.13_11-jre
WORKDIR /app
COPY --from=libs /app/ /app/
