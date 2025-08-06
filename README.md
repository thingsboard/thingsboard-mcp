# ThingsBoard MCP Server

[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://github.com/thingsboard/mcp-server/blob/master/README.md)

## Overview

The **ThingsBoard MCP Server** provides a **natural language interface** for LLMs and agentic applications to view, update, and analyze data and entities on the ThingsBoard Platform.  
It integrates seamlessly with **MCP (Model Context Protocol)** clients—such as Claude Desktop or Cursor—enabling AI-powered workflows to interact directly with your ThingsBoard instance.

## Features

- **Entity Operations**: View devices, assets, and customers from ThingsBoard.
- **Telemetry Access**: Retrieve attributes and time-series data for devices, assets, or customers.
- **Telemetry Insert/Update**: Save attributes or time-series data for devices, assets, or customers in ThingsBoard.
- **Relations**: Discover relationships between devices, assets, and customers.
- **Alarms**: Fetch alarms related to specific entities from ThingsBoard.

## Installation

The ThingsBoard MCP Server currently supports the [`stdio` transport](https://modelcontextprotocol.io/docs/concepts/transports#standard-input%2Foutput-stdio).  
Support for `streamable-http` transport will be added in a future release.

### Using Docker

You can run the server using Docker. Either build the image locally or use the official image from Docker Hub:  
[ThingsBoard MCP Docker](https://hub.docker.com/r/mcp/thingsboard)

#### Build the Docker Image

```bash
docker build -t mcp-thingsboard .
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

| Variable               | Description                                     |
|------------------------|-------------------------------------------------|
| `THINGSBOARD_URL`      | The base URL of your ThingsBoard instance       |
| `THINGSBOARD_USERNAME` | Username used to authenticate with ThingsBoard  |
| `THINGSBOARD_PASSWORD` | Password used to authenticate with ThingsBoard  |

These variables can be set either:

- Directly via Docker command line using the `-e` flag
- Or through the `env` configuration block in your MCP client setup (e.g., Claude Desktop)