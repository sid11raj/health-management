# Backend: build with Maven, run on a slim JRE image (no build tools at runtime).
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN mkdir -p /app/uploads
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
# All secrets/config come from the environment (see README). Do not bake
# JWT_SECRET, DB_PASSWORD or mail credentials into this image.
ENTRYPOINT ["java", "-jar", "app.jar"]
