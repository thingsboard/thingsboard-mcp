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

    public static ThingsBoardEdition fromType(String name) {
        if (name == null) {
            return CE;
        }
        String normalized = name.trim().toUpperCase();
        if ("PAAS".equals(normalized)) {
            return PE;
        }
        try {
            return ThingsBoardEdition.valueOf(normalized);
        } catch (Exception ignored) {
            return CE;
        }
    }

}
