FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080

# -XX:MaxRAMPercentage=75  Cap the heap at 75% of the container's memory
#   limit (512 MB on Koyeb's free instance) so the JVM sizes itself from
#   the cgroup limit instead of the host's full memory.
# -XX:TieredStopAtLevel=1  Stop at C1, skip C2. On a single shared vCPU
#   there's no spare core for C2's background compilation anyway, and
#   C1-only is well known to start up faster at the cost of steady-state
#   throughput this low-traffic API doesn't need.
# -XX:+UseSerialGC  Use the single-threaded serial collector. It is
#   correct at any heap size and is what the JVM itself recommends below
#   ~2 CPUs / 1-2 GB heap, avoiding the extra GC worker threads Parallel
#   or G1 would spin up on a 0.25-1 vCPU instance.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-XX:TieredStopAtLevel=1", "-XX:+UseSerialGC", "-jar", "app.jar"]
