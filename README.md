# ThingsBoard MCP Server

## Prerequisites

- Java 17 or later
- Maven 3.6 or later
- Understanding of Spring Boot and Spring AI concepts
- (Optional) Claude Desktop for AI assistant integration

## Project Structure

```
src/
├── main/
│   ├── java/
│   │   └── org/springframework/ai/mcp/sample/server/
│   │       ├── McpServerApplication.java    # Main application class with tool registration
│   │       └── WeatherService.java          # Weather service implementation with MCP tools
│   └── resources/
│       └── application.properties           # Server and transport configuration
└── test/
    └── java/
        └── org/springframework/ai/mcp/sample/client/
            └── ClientStdio.java             # Test client implementation
```

## Building and Running

The server uses STDIO transport mode and is typically started automatically by the client. To build the server jar:

```bash
mvn clean package -DskipTests
```
