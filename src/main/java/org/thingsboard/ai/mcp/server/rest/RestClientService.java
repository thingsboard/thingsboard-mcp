package org.thingsboard.ai.mcp.server.rest;

import org.thingsboard.rest.client.RestClient;

public interface RestClientService {
    RestClient getClient();
}
