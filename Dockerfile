# ---------- Stage 1: build the jar ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Download dependencies first so this layer is cached until pom.xml changes
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q package -DskipTests

# ---------- Stage 2: small runtime image ----------
FROM eclipse-temurin:17-jre
WORKDIR /app

# Do not run as root
RUN groupadd --system spring && useradd --system --gid spring spring
USER spring

COPY --from=build /app/target/university-management-system.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
