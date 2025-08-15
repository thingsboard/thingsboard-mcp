# Contributing to ThingsBoard MCP

Thanks for your interest in contributing!  
This project implements a Model Context Protocol (MCP) server for [ThingsBoard](https://thingsboard.io) with tools for Devices, Assets, Telemetry, Alarms, Admin, and more.

We welcome contributions for:
- New MCP tools or enhancements to existing ones
- Bug fixes
- Performance improvements
- Documentation updates
- CI/CD and build improvements

---

## 📋 Code of Conduct
Please review our [Code of Conduct](CODE_OF_CONDUCT.md) before contributing. By participating, you agree to abide by it.

---

## 🛠 Development Setup

### Prerequisites
- **Java 17**
- **Maven 3.6+**
- Docker (optional, for container testing)
- Git

### Build
```bash
mvn -B clean verify
```

### Run (STDIO mode)
```bash
java -jar ./target/thingsboard-mcp-server-*.jar
```

### Run (SSE/HTTP mode)
```bash
java -Dspring.ai.mcp.server.stdio=false -Dspring.main.web-application-type=servlet -jar ./target/thingsboard-mcp-server-*.jar
```

### Environment Variables
These are required for the MCP server to connect to a ThingsBoard instance:
```
THINGSBOARD_URL=<url>
THINGSBOARD_USERNAME=<username>
THINGSBOARD_PASSWORD=<password>
```
Optional:
- `SPRING_AI_MCP_SERVER_STDIO` – disable for SSE mode
- `SERVER_PORT` – HTTP port for SSE mode (default 8080)

---

## 🧪 Testing Your Changes
1. **Unit tests**: Ensure all existing and new tests pass.
```bash
mvn test
```

2. **Integration smoke test**:
    - Run the server in your chosen mode
    - Connect with an MCP client (e.g., Claude Desktop, Cursor)
    - Exercise affected tools and verify responses

3. **Docker test** (if changes affect container build/runtime):
```bash
docker build -t my-mcp .
docker run --rm -i -e THINGSBOARD_URL=<url> -e THINGSBOARD_USERNAME=<user> -e THINGSBOARD_PASSWORD=<pass> my-mcp
```

---

## 📝 Pull Request Guidelines
- Use the [Pull Request Template](.github/PULL_REQUEST_TEMPLATE.md)
- Keep commits focused and well-described
- Update relevant documentation (README, examples)
- Reference related issues (e.g., `Closes #123`)
- Ensure code follows our formatting and style

---

## 🐛 Reporting Bugs
- Use the [Bug Report Template](.github/ISSUE_TEMPLATE/bug_report.md)
- Include:
    - MCP mode (STDIO/SSE)
    - Run method (Docker/JAR)
    - Java version, OS, ThingsBoard version
    - Minimal reproduction steps
    - Logs (sanitized)

---

## 💡 Requesting Features
- Use the [Feature Request Template](.github/ISSUE_TEMPLATE/feature_request.md)
- Describe:
    - Problem/use case
    - Proposed solution
    - Alternatives considered
    - Scope (tool, mode, docs, etc.)

---

## 🔧 Adding a New MCP Tool
1. Choose a category (Device, Asset, Customer, User, Alarm, Entity Group, Relation, Telemetry, Admin)
2. Implement tool logic using ThingsBoard REST API
3. Add schema for input/output
4. Write unit tests
5. Update README and examples
6. Submit via PR with the **New MCP Tool** template

---

## 📜 License
By contributing, you agree that your contributions will be licensed under the [Apache License 2.0](LICENSE).

---

Happy coding 🚀
