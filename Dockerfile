FROM thingsboard/openjdk17:bookworm-slim

WORKDIR /app

COPY ./target/mcp-thingsboard-server-4.0.0-SNAPSHOT.jar app.jar

# Set environment variable for stdio
ENV SPRING_AI_MCP_SERVER_STDIO=true

# Entry point for Claude MCP
ENTRYPOINT ["java", "-Dspring.ai.mcp.server.stdio=true", "-jar", "app.jar"]
