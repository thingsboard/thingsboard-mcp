# Stage 1: Build the project using Maven
FROM maven:3.9.4-eclipse-temurin-21 AS builder

WORKDIR /app
COPY . .

RUN mvn clean package -DskipTests

# Stage 2: Runtime container
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy the built jar from the builder stage
COPY --from=builder /app/target/mcp-thingsboard-server-1.0.0.jar app.jar

# Optional JVM extras at runtime: -Xmx, debug agent, log levels, etc.
ENV JAVA_OPTS=""

# Let Spring read env vars from application.yml (${VAR:default})
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
