# Stage 1: Build the project using Maven
FROM maven:3.9.4-eclipse-temurin-21 AS builder

WORKDIR /app
COPY . .

RUN mvn clean package -DskipTests

# Stage 2: Runtime container
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy the built jar from the builder stage (using wildcard to match any version)
COPY --from=builder /app/target/thingsboard-mcp-server-*.jar app.jar

# Optional JVM extras at runtime: -Xmx, debug agent, log levels, etc.
ENV JAVA_OPTS=""

# Bind to all interfaces so the server is reachable from outside the container
ENV HTTP_BIND_ADDRESS="0.0.0.0"

# Let Spring read env vars from application.yml (${VAR:default})
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
