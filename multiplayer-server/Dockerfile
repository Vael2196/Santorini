FROM maven:3.9.11-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml ./
COPY game-core/ game-core/
COPY multiplayer-server/ multiplayer-server/
COPY desktop-client/ desktop-client/
COPY Santorini/src/ Santorini/src/
RUN mvn -B -pl multiplayer-server -am package -DskipTests

FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S game && adduser -S game -G game
WORKDIR /app
RUN mkdir -p /app/data && chown game:game /app/data
COPY --from=build /app/multiplayer-server/target/multiplayer-server-1.0.0-SNAPSHOT.jar server.jar
USER game
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=65", "-jar", "server.jar"]
