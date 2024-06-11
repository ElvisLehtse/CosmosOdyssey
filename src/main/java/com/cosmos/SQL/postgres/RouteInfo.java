package com.cosmos.SQL.postgres;

public class RouteInfo {

    String uuid;
    String priceListUuid;
    String fromPlanetUuid;
    String toPlanetUuid;
    Long distance;

    public String getUuid() {
        return uuid;
    }

    public String getPriceListUuid() {
        return priceListUuid;
    }

    public String getFromPlanetUuid() {
        return fromPlanetUuid;
    }

    public String getToPlanetUuid() {
        return toPlanetUuid;
    }

    public Long getDistance() {
        return distance;
    }

    public RouteInfo (String uuid, String priceListUuid, String fromPlanetUuid, String toPlanetUuid, Long distance) {
        this.uuid = uuid;
        this.priceListUuid = priceListUuid;
        this.fromPlanetUuid = fromPlanetUuid;
        this.toPlanetUuid = toPlanetUuid;
        this.distance = distance;
    }
}
