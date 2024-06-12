package com.cosmos.SQL.postgres;

import com.cosmos.SQL.postgres.initiator.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

public class BestDealCalculator {

    public BestDealCalculator (Connection connection) throws SQLException {
        InitiateLists initiateLists = new InitiateLists(connection);
        routeList = initiateLists.getRouteList();
        planetList = initiateLists.getPlanetList();
        providerList = initiateLists.getProviderList();
        companyList = initiateLists.getCompanyList();
    }
    static List<RouteInfo> routeList = new ArrayList<>();
    static List<Planet> planetList = new ArrayList<>();
    static List<Provider> providerList = new ArrayList<>();
    static List<Company> companyList = new ArrayList<>();

    public void generateSolutions(String originPlanet, String destinationPlanet, List<String> companyList) throws SQLException{
        List<String> currentPath = new ArrayList<>();
        List<String> routeUuid = new ArrayList<>();
        List<List<String>> allRouteUuid = new ArrayList<>();

        findAllPossibleRoutes(originPlanet, destinationPlanet, routeUuid, currentPath, allRouteUuid, null);
        List<List<Provider>> priceList = new ArrayList<>();
        allRouteUuid = filterByCompany(companyList, allRouteUuid, priceList);
        prettyPrintPaths(getPaths(originPlanet, allRouteUuid), originPlanet, destinationPlanet, getLowestPrice(priceList), companyList);
    }

    private void findAllPossibleRoutes(String originPlanet, String destinationPlanet, List<String> currentPath, List<String> routeUuid, List<List<String>> allRouteUuid, String uuid) {
        currentPath.add(originPlanet);
        if (uuid != null) {
            routeUuid.add(uuid);
        }
        if (originPlanet.equals(destinationPlanet)) {
            allRouteUuid.add(routeUuid);
        }
        for (RouteInfo route : routeList) {
            if (route.getOriginPlanetUuid().equals(originPlanet)) {
                uuid = route.getUuid();
                String destination = route.getDestinationPlanetUuid();
                if (!currentPath.contains(destination)) {
                    findAllPossibleRoutes(destination, destinationPlanet, new ArrayList<>(currentPath), new ArrayList<>(routeUuid), allRouteUuid, uuid);
                }
            }
        }
    }

    private List<List<String>> filterByCompany(List<String> companyList, List<List<String>> allRouteUuid, List<List<Provider>> priceList) {
        List<Provider> suitableProviders = new ArrayList<>();
        List<List<String>> suitableRoutesUuid = new ArrayList<>();
        List<List<String>> newAllRoutesUuid = new ArrayList<>();
        for (int i = 0; i < providerList.size(); i++) {
            if (companyList.contains(providerList.get(i).getCompany_uuid())) {
                suitableProviders.add(providerList.get(i));
            }
        }

        for (int j = 0; j < allRouteUuid.size(); j++) {
            List<Provider> tempPrice = new ArrayList<>();
            List<String> tempList = new ArrayList<>();
            for (int k = 0; k < allRouteUuid.get(j).size(); k++) {
                for (int i = 0; i < suitableProviders.size(); i++) {

                    if (allRouteUuid.get(j).get(k).equals(suitableProviders.get(i).getRoute_info_uuid())) {
                        if (!tempList.contains(suitableProviders.get(i).getRoute_info_uuid())) {
                            tempList.add(suitableProviders.get(i).getRoute_info_uuid());
                        }
                        if (!tempPrice.contains(suitableProviders.get(i))) {
                            tempPrice.add(suitableProviders.get(i));
                        }
                    }
                }
            }
            if (!tempPrice.isEmpty() && !priceList.contains(tempPrice)) {
                priceList.add(tempPrice);
            }
            if (!tempList.isEmpty() && !suitableRoutesUuid.contains(tempList)) {
                suitableRoutesUuid.add(tempList);
            }
        }

        for (int i = 0; i < allRouteUuid.size(); i++) {
            if (allRouteUuid.get(i).equals(suitableRoutesUuid.get(i))) {
                newAllRoutesUuid.add(allRouteUuid.get(i));
            }
        }
        return newAllRoutesUuid;
    }

    private static List<List<String>> getPaths(String originPlanet, List<List<String>> allRouteUuid) {
        List<List<String>> allPaths = new ArrayList<>();
        for (int i = 0; i < allRouteUuid.size(); i++) {
            List<String> path = new ArrayList<>();
            path.add(originPlanet);
            for (int j = 0; j < allRouteUuid.get(i).size(); j++) {
                for (int k = 0; k < routeList.size(); k++) {
                    if (allRouteUuid.get(i).get(j).equals(routeList.get(k).getUuid())) {
                        path.add(routeList.get(k).getDestinationPlanetUuid());
                    }
                }
            }
            allPaths.add(path);
        }
        return allPaths;
    }


    private List<Long> getLowestPrice(List<List<Provider>> priceList) {
        List<Long> totalPrice = new ArrayList<>();

        for (int i = 0; i < priceList.size(); i++) {
            long tempPrice = 0;
            Map<String, Long> lowestValue = new HashMap<>();
            for (int j = 0; j < priceList.get(i).size(); j++) {
                if (lowestValue.containsKey(priceList.get(i).get(j).getRoute_info_uuid())) {
                    if (priceList.get(i).get(j).getPrice() < lowestValue.get(priceList.get(i).get(j).getRoute_info_uuid())) {
                        lowestValue.put(priceList.get(i).get(j).getRoute_info_uuid(), priceList.get(i).get(j).getPrice());
                    }
                } else {
                    lowestValue.put(priceList.get(i).get(j).getRoute_info_uuid(), priceList.get(i).get(j).getPrice());
                }
            }
            for (Map.Entry<String, Long> entry : lowestValue.entrySet()) {
                tempPrice = tempPrice + entry.getValue();
            }
            totalPrice.add(tempPrice);
        }
        return totalPrice;
    }

    private void prettyPrintPaths(List<List<String>> allPaths, String originPlanet, String destinationPlanet, List<Long> totalPrice, List<String> companyList) {
        List<String> prettyPrint = new ArrayList<>();
        for (int i = 0; i < allPaths.size(); i++) {
            List<String> allPath = allPaths.get(i);
            List<String> tempList = new ArrayList<>();
            for (int j = 0; j < allPath.size(); j++) {
                String path = allPath.get(j);
                for (int k = 0; k < planetList.size(); k++) {
                    Planet planet = planetList.get(k);
                    if (planet.getUuid().equals(path)) {
                        tempList.add(planet.getName());
                        break;
                    }
                }
            }
            prettyPrint.add(String.join(" | total lowest cost: ", String.join(" -> ", tempList), String.join("", String.valueOf(totalPrice.get(i)))));
        }

        String origin = "";
        String destination = "";
        for (Planet planet : planetList) {
            if (planet.getUuid().equals(originPlanet)) {
                origin = planet.getName();
                break;
            }
        }
        for (Planet planet : planetList) {
            if (planet.getUuid().equals(destinationPlanet)) {
                destination = planet.getName();
                break;
            }
        }

        System.out.println(STR."All possible routes from \{origin} to \{destination}:");
        for (String path : prettyPrint) {
            System.out.println(path);
        }
        System.out.println("When traveling with companies: ");
    }
}



