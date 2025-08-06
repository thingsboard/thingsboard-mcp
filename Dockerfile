# Stage 1: Build the project using Maven
FROM maven:3.9.4-eclipse-temurin-21 AS builder

WORKDIR /app
COPY . .

RUN mvn clean package -DskipTests

# Stage 2: Runtime container
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy the built jar from the builder stage
COPY --from=builder /app/target/mcp-thingsboard-server-4.0.0-SNAPSHOT.jar app.jar

# Set environment variable for stdio
ENV SPRING_AI_MCP_SERVER_STDIO=true

# Entry point for Claude MCP
ENTRYPOINT ["java", "-Dspring.ai.mcp.server.stdio=true", "-jar", "app.jar"]