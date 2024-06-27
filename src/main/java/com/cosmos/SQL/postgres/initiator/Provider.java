package com.cosmos.SQL.postgres.initiator;

import java.sql.Timestamp;
import java.time.Duration;

public record Provider(String uuid, String company_uuid, String route_info_uuid, Long price, Timestamp flight_start, Timestamp flight_end) {

    public long getTravelTime() {
        return Duration.between(flight_start().toLocalDateTime(), flight_end().toLocalDateTime()).toMinutes();
    }
}
