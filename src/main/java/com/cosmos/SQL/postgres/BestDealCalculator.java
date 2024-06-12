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
        List<List<Provider>> suitableProvidersByRoute = new ArrayList<>();
        allRouteUuid = filterByCompany(companyList, allRouteUuid, suitableProvidersByRoute);
        prettyPrintPaths(getPaths(originPlanet, allRouteUuid), originPlanet, destinationPlanet, getLowestPrice(suitableProvidersByRoute));
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

    private List<List<String>> filterByCompany(List<String> companyList, List<List<String>> allRouteUuid, List<List<Provider>> suitableProvidersByRoute) {
        List<Provider> suitableProviders = new ArrayList<>();
        List<List<String>> suitableRoutesUuid = new ArrayList<>();
        List<List<String>> newAllRoutesUuid = new ArrayList<>();
        for (int i = 0; i < providerList.size(); i++) {
            if (companyList.contains(providerList.get(i).getCompany_uuid())) {
                suitableProviders.add(providerList.get(i));
            }
        }

        for (int j = 0; j < allRouteUuid.size(); j++) {
            List<Provider> tempListForSuitableProvidersByRoute = new ArrayList<>();
            List<String> tempListForSuitableRoutesUuid = new ArrayList<>();
            for (int k = 0; k < allRouteUuid.get(j).size(); k++) {
                for (int i = 0; i < suitableProviders.size(); i++) {

                    if (allRouteUuid.get(j).get(k).equals(suitableProviders.get(i).getRoute_info_uuid())) {
                        if (!tempListForSuitableRoutesUuid.contains(suitableProviders.get(i).getRoute_info_uuid())) {
                            tempListForSuitableRoutesUuid.add(suitableProviders.get(i).getRoute_info_uuid());
                        }
                        if (!tempListForSuitableProvidersByRoute.contains(suitableProviders.get(i))) {
                            tempListForSuitableProvidersByRoute.add(suitableProviders.get(i));
                        }
                    }
                }
            }
            if (!tempListForSuitableProvidersByRoute.isEmpty() && !suitableProvidersByRoute.contains(tempListForSuitableProvidersByRoute)) {
                suitableProvidersByRoute.add(tempListForSuitableProvidersByRoute);
            }
            if (!tempListForSuitableRoutesUuid.isEmpty() && !suitableRoutesUuid.contains(tempListForSuitableRoutesUuid)) {
                suitableRoutesUuid.add(tempListForSuitableRoutesUuid);
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


    private List<List<Provider>> getLowestPrice(List<List<Provider>> suitableProvidersByRoute) {
        List<List<Provider>> providerListWithSelectedCompanies = new ArrayList<>();
        for (int i = 0; i < suitableProvidersByRoute.size(); i++) {
            List<Provider> tempList = new ArrayList<>();
            for (int j = 0; j < suitableProvidersByRoute.get(i).size(); j++) {
                tempList.add(suitableProvidersByRoute.get(i).get(j));
                for (int k = 0; k < tempList.size(); k++) {
                    if (tempList.get(k).getRoute_info_uuid().equals(suitableProvidersByRoute.get(i).get(j).getRoute_info_uuid())) {
                        if (tempList.get(k).getPrice() > suitableProvidersByRoute.get(i).get(j).getPrice()) {
                            tempList.remove(k);
                        } else if (tempList.get(k).getPrice() < suitableProvidersByRoute.get(i).get(j).getPrice()) {
                            tempList.removeLast();
                        }
                    }
                }
            }
            providerListWithSelectedCompanies.add(tempList);
        }
        return providerListWithSelectedCompanies;
    }

    private void prettyPrintPaths(List<List<String>> allPaths, String originPlanet, String destinationPlanet, List<List<Provider>> providerListWithSelectedCompanies) {

        List<Long> totalPrice = new ArrayList<>();
        for (int i = 0; i < providerListWithSelectedCompanies.size(); i++) {
            long tempPrice = 0;
            for (int j = 0; j < providerListWithSelectedCompanies.get(i).size(); j++) {
                tempPrice = tempPrice + providerListWithSelectedCompanies.get(i).get(j).getPrice();
            }
            totalPrice.add(tempPrice);
        }

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

        List<String> selectedCompanies = new ArrayList<>();
        for (int i = 0; i < providerListWithSelectedCompanies.size(); i++) {
            for (int j = 0; j < providerListWithSelectedCompanies.get(i).size(); j++) {
                if (!selectedCompanies.contains(providerListWithSelectedCompanies.get(i).get(j).getCompany_uuid())) {
                    selectedCompanies.add(providerListWithSelectedCompanies.get(i).get(j).getCompany_uuid());
                }
            }
        }

        System.out.println(STR."All possible routes from \{origin} to \{destination}:");
        for (String path : prettyPrint) {
            System.out.println(path);
        }

        System.out.println("When traveling with companies: ");
        for (int i = 0; i < selectedCompanies.size(); i++) {
            System.out.println(selectedCompanies.get(i));
        }
    }
}



