package org.thingsboard.ai.mcp.server.data;

import lombok.Getter;

public enum ThingsBoardEdition {

    CE("Community edition"),
    PE("Professional edition");

    @Getter
    private final String name;

    ThingsBoardEdition(String name) {
        this.name = name;
    }

}
