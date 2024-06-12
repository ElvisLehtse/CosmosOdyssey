package com.cosmos.SQL.postgres;

import java.sql.Timestamp;

public class Provider {

    private final String uuid;
    private final String company_uuid;
    private final String route_info_uuid;
    private final int price;
    private final Timestamp flight_start;
    private final Timestamp flight_end;

    public String getUuid() {
        return uuid;
    }

    public String getCompany_uuid() {
        return company_uuid;
    }

    public String getRoute_info_uuid() {
        return route_info_uuid;
    }

    public int getPrice() {
        return price;
    }

    public Timestamp getFlight_start() {
        return flight_start;
    }

    public Timestamp getFlight_end() {
        return flight_end;
    }

    public Provider (String uuid, String company_uuid, String route_info_uuid, int price, Timestamp flight_start, Timestamp flight_end) {
        this.uuid = uuid;
        this.company_uuid = company_uuid;
        this.route_info_uuid = route_info_uuid;
        this.price = price;
        this.flight_start = flight_start;
        this.flight_end = flight_end;
    }
}
