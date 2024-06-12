package com.cosmos.SQL.postgres;

public class RouteInfo {

    private final String uuid;
    private final String priceListUuid;
    private final String from_planet_uuid;
    private final String to_planet_uuid;
    private final Long distance;

    public String getUuid() {
        return uuid;
    }

    public String getPriceListUuid() {
        return priceListUuid;
    }

    public String getOriginPlanetUuid() {
        return from_planet_uuid;
    }

    public String getDestinationPlanetUuid() {
        return to_planet_uuid;
    }

    public Long getDistance() {
        return distance;
    }

    public RouteInfo (String uuid, String priceListUuid, String fromPlanetUuid, String toPlanetUuid, Long distance) {
        this.uuid = uuid;
        this.priceListUuid = priceListUuid;
        this.from_planet_uuid = fromPlanetUuid;
        this.to_planet_uuid = toPlanetUuid;
        this.distance = distance;
    }
}
