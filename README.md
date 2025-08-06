# ThingsBoard MCP Server

[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://github.com/thingsboard/mcp-server/blob/master/README.md)

## Overview

The **ThingsBoard MCP Server** provides a **natural language interface** for LLMs and agentic applications to view, update, and analyze data and entities on the ThingsBoard Platform.  
It integrates seamlessly with **MCP (Model Context Protocol)** clients—such as Claude Desktop or Cursor—enabling AI-powered workflows to interact directly with your ThingsBoard instance.

## Features

- **Entity Operations**
  - **Devices**: View device details, credentials, profiles, and manage device relationships
  - **Assets**: View and manage assets, asset profiles, and asset relationships
  - **Customers**: Access customer information, titles, and manage customer relationships
  - **Users**: Manage users, tokens, activation links, and user assignments
- **Telemetry Management**
  - **Attribute Access**: Retrieve attribute keys and values by scope for any entity
  - **Time-series Access**: Get time-series data with various aggregation options
  - **Telemetry Insert/Update**: Save attributes or time-series data with optional TTL settings
- **Relations**: Discover and navigate relationships between entities with direction-based queries
- **Alarms**: Fetch alarms, alarm types, and severity information for specific entities
- **Administration**
  - **System Settings**: Access and manage administration settings
  - **Security Settings**: View security policies and JWT configuration
  - **Version Control**: Manage repository and auto-commit settings
  - **System Information**: Check for updates and retrieve usage statistics

## Installation

The ThingsBoard MCP Server currently supports the [`stdio` transport](https://modelcontextprotocol.io/docs/concepts/transports#standard-input%2Foutput-stdio).  
Support for `streamable-http` transport will be added in a future release.

### Using Docker

You can run the server using Docker. Either build the image locally or use the official image from Docker Hub:  
[ThingsBoard MCP Docker](https://hub.docker.com/r/mcp/thingsboard)

#### Build the Docker Image

```bash
docker build -t mcp/thingsboard .
```

#### Example Configuration (Claude Desktop)

To launch the server as a container when your MCP client starts (e.g., Claude Desktop), add the following entry to your `claude_desktop_config.json`:

```json
{
  "mcpServers": {
    "thingsboard": {
      "command": "docker",
      "args": [
        "run",
        "-i",
        "--rm",
        "-e",
        "THINGSBOARD_URL",
        "-e",
        "THINGSBOARD_USERNAME",
        "-e",
        "THINGSBOARD_PASSWORD",
        "mcp/thingsboard"
      ],
      "env": {
        "THINGSBOARD_URL": "<thingsboard_url>"
      },
      "secret": {
        "THINGSBOARD_USERNAME": "<thingsboard_username>",
        "THINGSBOARD_PASSWORD": "<thingsboard_password>"
      }
    }
  }
}
```

## Environment Variables

The MCP server requires the following environment variables to connect to your ThingsBoard instance:

| Variable                             | Description                                               | Default |
|--------------------------------------|-----------------------------------------------------------|---------|
| `THINGSBOARD_URL`                    | The base URL of your ThingsBoard instance                 |         |
| `THINGSBOARD_USERNAME`               | Username used to authenticate with ThingsBoard            |         |
| `THINGSBOARD_PASSWORD`               | Password used to authenticate with ThingsBoard            |         |
| `THINGSBOARD_LOGIN_INTERVAL_SECONDS` | Login session refresh interval in seconds                 | 1800    |

These variables can be set either:

- Directly via Docker command line using the `-e` flag
- Or through the `env` configuration block in your MCP client setup (e.g., Claude Desktop)