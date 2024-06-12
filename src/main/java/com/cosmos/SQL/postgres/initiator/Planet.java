package com.cosmos.SQL.postgres.initiator;

public class Planet {
    private final String uuid;
    private final String name;

    public String getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public Planet (String uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }
}
