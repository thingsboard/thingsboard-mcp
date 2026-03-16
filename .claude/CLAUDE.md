# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview
Spring Boot MCP (Model Context Protocol) server that exposes ThingsBoard IoT platform operations as tools for LLMs. Supports STDIO and SSE transport modes. Built on Spring AI MCP Server (`spring-ai-starter-mcp-server-webmvc` 1.1.2) with ThingsBoard REST client 4.3.0PE.

## Build & Test
```bash
mvn clean install -DskipTests    # Build
mvn test                          # Run all tests (171 tests)
mvn test -Dtest=AlarmToolsTest    # Run single test class
mvn package -DskipTests           # Build JAR only
```
- Java 25, Maven 3.6+
- JAR output: `target/thingsboard-mcp-server-2.2.0.jar`

## Run
```bash
# STDIO mode (default)
java -jar target/thingsboard-mcp-server-2.2.0.jar

# SSE mode (HTTP on port 8000)
java -Dspring.ai.mcp.server.stdio=false -Dspring.main.web-application-type=servlet -jar target/thingsboard-mcp-server-2.2.0.jar
```
Required env vars: `THINGSBOARD_URL`, `THINGSBOARD_API_KEY` (or legacy `THINGSBOARD_USERNAME`/`THINGSBOARD_PASSWORD`)

## Architecture

### Entry Point
`McpServerApplication.java` — Spring Boot app that wires `EditionAwareToolProvider` as a `ToolCallbackProvider` bean to expose tools via MCP.

### Tool Structure
Each tool group is a `@Service` class implementing `McpTools` (marker interface), annotated with `@ToolGroup("<name>")`. Methods annotated with `@Tool` become MCP tools. Parameters use `@ToolParam`.

| Group | Class | Package |
|-------|-------|---------|
| `device` | `DeviceTools`, `DeviceCreateTools` | `tools/device/` |
| `asset` | `AssetTools` | `tools/asset/` |
| `customer` | `CustomerTools` | `tools/customer/` |
| `user` | `UserTools` | `tools/user/` |
| `alarm` | `AlarmTools` | `tools/alarm/` |
| `ota` | `OtaTools` | `tools/ota/` |
| `relation` | `RelationTools` | `tools/relation/` |
| `telemetry` | `TelemetryTools` | `tools/telemetry/` |
| `edq` | `EntityQueryTools`, `GuideTools` | `tools/query/` |
| `group` | `EntityGroupTools` | `tools/group/` |

Tool groups can be enabled/disabled via `THINGSBOARD_TOOLS_GROUPS_<GROUP>=true|false` env vars (see `ToolGroupsProperties`). Groups not configured default to enabled.

### Key Classes
- `RestClientService` — manages ThingsBoard REST client, handles auth (API key preferred, username/password legacy). Publishes `EditionChangedEvent` on connect.
- `EditionAwareToolProvider` — implements `ToolCallbackProvider`. Filters tools by ThingsBoard edition (CE vs PE) using `@PeOnly`/`@CeOnly` method annotations, and by group using `@ToolGroup` class annotation + `ToolGroupsProperties`.
- `ControllerConstants` — shared parameter descriptions for `@ToolParam`, entity filter JSON templates, and EDQ query structure constants.
- `ToolUtils` — helpers for parsing page links, key filters, entity keys.

### Edition Filtering
- `@PeOnly` on a tool method — only available when connected to PE (Professional Edition)
- `@CeOnly` on a tool method — only available when connected to CE (Community Edition)
- On edition change, `EditionAwareToolProvider` publishes `RemoveToolsEvent` to remove incompatible tools.

### EDQ (Entity Data Query)
The most complex tool group (~37 tools). Two design patterns:

1. **Routing hints**: `EDQ_ROUTING` constant prefixes every EDQ tool description to tell LLMs WHEN to prefer EDQ over simple list/get+telemetry tools: "Prefer over simple list/get tools when you need to filter entities by attribute/telemetry values."

2. **Guide tools**: `getEdqGuide()`, `getEdqCountGuide()`, `getKeyFiltersGuide()` return markdown docs from `src/main/resources/guide/`. EDQ find tools append `EDQ_GUIDES` ("Call getEdqGuide() and getKeyFiltersGuide() first.") and count tools append `COUNT_GUIDES` ("Call getEdqCountGuide() and getKeyFiltersGuide() first.") to encourage LLMs to read documentation before constructing queries.

Each EDQ tool targets a specific entity filter type (e.g., `findEntityDataByDeviceTypeFilter`, `countByAssetTypeFilter`). The entity filter JSON template is in `ControllerConstants` (e.g., `DEVICE_TYPE`, `ASSET_TYPE`, `RELATIONS_QUERY_FILTER`).

## Tool Description Conventions
- Frontload with "Use this to..." pattern
- All descriptions must be under 1024 chars (Azure Foundry limit)
- No trailing whitespace in descriptions or param descriptions (causes Azure schema errors)
- No empty `properties` in tool schemas (add unused param if needed — Azure rejects empty properties)
- EDQ tools: prefix with `EDQ_ROUTING` constant for routing hints
- Remove boilerplate (authority paragraphs, security checks, `PAGE_DATA_PARAMETERS`) — zero signal for LLM tool selection
- Tool names must be under 64 chars

## Config
- `src/main/resources/application.yml` — server config, tool groups, ThingsBoard connection
- `src/main/resources/guide/` — EDQ documentation (key-filter.md, edq-guide.md, edq-count-guide.md)

## Tests
Located in `src/test/java/org/thingsboard/ai/mcp/server/service/tools/`. Each tool class has a corresponding test. Tests mock `RestClientService` and verify tool behavior (return values, API calls), not description text.

## Branches
- `master` — stable release branch
