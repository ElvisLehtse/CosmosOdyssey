package com.cosmos.SQL.postgres;

public class Company {

    String uuid;
    String name;

    public String getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public Company (String uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }
}
