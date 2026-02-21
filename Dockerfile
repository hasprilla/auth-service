# Stage 1: Build
FROM gradle:8.5.0-jdk21 AS build
WORKDIR /home/gradle/src
COPY --chown=gradle:gradle build.gradle settings.gradle ./
COPY --chown=gradle:gradle src ./src
RUN gradle build --no-daemon -x test

# Stage 2: Run
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S sonifoy && adduser -S sonifoy -G sonifoy
RUN mkdir -p /app/logs && chown -R sonifoy:sonifoy /app
USER sonifoy:sonifoy
COPY --from=build /home/gradle/src/build/libs/*.jar app.jar
ENTRYPOINT ["java","-jar","/app/app.jar"]
