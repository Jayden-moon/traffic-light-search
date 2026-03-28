FROM eclipse-temurin:17-jdk AS build

WORKDIR /app
COPY gradle gradle
COPY gradlew .
COPY build.gradle .
COPY settings.gradle .
COPY trafficlight-domain trafficlight-domain
COPY trafficlight-usecase trafficlight-usecase
COPY trafficlight-data trafficlight-data
COPY trafficlight-api trafficlight-api

RUN chmod +x gradlew && ./gradlew :trafficlight-api:bootJar --no-daemon -x test

FROM eclipse-temurin:17-jre

WORKDIR /app

RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser

COPY --from=build /app/trafficlight-api/build/libs/*.jar app.jar
COPY data/ /app/data/

RUN chown -R appuser:appgroup /app

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
