FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8087
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s \
  CMD curl -sf http://localhost:8087/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
