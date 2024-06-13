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

    public void generateSolutions(String originPlanet, String destinationPlanet, List<String> companyListProvidedByUser) throws SQLException{
        List<String> currentPath = new ArrayList<>();
        List<String> routeUuid = new ArrayList<>();
        List<List<String>> allPossibleRoutes = findAllPossibleRoutes(originPlanet, destinationPlanet, routeUuid, currentPath, new ArrayList<>(), null);
        List<List<Provider>> suitableProvidersByRoute = filterByCompany(companyListProvidedByUser, allPossibleRoutes);
        List<Long> routeDistance = routesDistance(allPossibleRoutes);
        List<List<String>> allPaths = getPaths(originPlanet, allPossibleRoutes);
        List<List<Provider>> suitableProvidersWithLowestCost = getLowestPrice(suitableProvidersByRoute);
        List<Long> totalPrice = totalPrice(suitableProvidersWithLowestCost);
        prettyPrintPaths(allPaths, originPlanet, destinationPlanet, suitableProvidersWithLowestCost, routeDistance, totalPrice);
    }

    /**
     * Finds all possible routes from origin planet to destination planet.
     * Routes are returned as lists within a list, containing route_info UUID-s.
     */

    private List<List<String>> findAllPossibleRoutes(String originPlanet, String destinationPlanet, List<String> currentPath,
                                       List<String> routeUuid, List<List<String>> allPossibleRoutes, String uuid) {
        currentPath.add(originPlanet);
        if (uuid != null) {
            routeUuid.add(uuid);
        }
        if (originPlanet.equals(destinationPlanet)) {
            allPossibleRoutes.add(routeUuid);
        }
        for (RouteInfo route : routeList) {
            if (route.getOriginPlanetUuid().equals(originPlanet)) {
                uuid = route.getUuid();
                String destination = route.getDestinationPlanetUuid();
                if (!currentPath.contains(destination)) {
                    findAllPossibleRoutes(destination, destinationPlanet, new ArrayList<>(currentPath),
                            new ArrayList<>(routeUuid), allPossibleRoutes, uuid);
                }
            }
        }
        return allPossibleRoutes;
    }

    /**
     * Filters all possible routes by companies provided by the end user.
     * Filtered routes are returned along with provider information as a list within a list,
     * containing provider uuid, company uuid, route info uuid, price, flight start and flight end information.
     */

    private List<List<Provider>> filterByCompany(List<String> companyListProvidedByUser, List<List<String>> allPossibleRoutes) {
        List<List<Provider>> suitableProvidersByRoute = new ArrayList<>();
        List<Provider> suitableProviders = new ArrayList<>();
        for (Provider provider : providerList) {
            if (companyListProvidedByUser.contains(provider.getCompany_uuid())) {
                suitableProviders.add(provider);
            }
        }
        for (List<String> allPossibleRoute : allPossibleRoutes) {
            List<Provider> tempListForSuitableProvidersByRoute = new ArrayList<>();
            for (String s : allPossibleRoute) {
                for (Provider suitableProvider : suitableProviders) {
                    if (s.equals(suitableProvider.getRoute_info_uuid())) {
                        if (!tempListForSuitableProvidersByRoute.contains(suitableProvider)) {
                            tempListForSuitableProvidersByRoute.add(suitableProvider);
                        }
                    }
                }
            }
            suitableProvidersByRoute.add(tempListForSuitableProvidersByRoute);
        }
        return suitableProvidersByRoute;
    }

    /**
     * Calculates the total distances for all possible routes.
     * Route distances are returned as a list.
     */

    private static List<Long> routesDistance(List<List<String>> allPossibleRoutes) {
        List<Long> routeDistance = new ArrayList<>();
        for (List<String> allPossibleRoute : allPossibleRoutes) {
            long tempList = 0L;
            for (String s : allPossibleRoute) {
                for (RouteInfo routeInfo : routeList) {
                    if (s.equals(routeInfo.getUuid())) {
                        tempList = tempList + routeInfo.getDistance();
                    }
                }
            }
            routeDistance.add(tempList);
        }
        return routeDistance;
    }

    /**
     * Route UUID info in all possible routes is converted into to planet UUID information
     * to refer to the destination planets.
     */

    private static List<List<String>> getPaths(String originPlanet, List<List<String>> allPossibleRoutes) {
        List<List<String>> allPaths = new ArrayList<>();
        for (List<String> allPossibleRoute : allPossibleRoutes) {
            List<String> path = new ArrayList<>();
            path.add(originPlanet);
            for (String uuid : allPossibleRoute) {
                for (RouteInfo routeInfo : routeList) {
                    if (uuid.equals(routeInfo.getUuid())) {
                        path.add(routeInfo.getDestinationPlanetUuid());
                    }
                }
            }
            allPaths.add(path);
        }
        return allPaths;
    }

    /**
     * Takes in the suitable Provider list and filters out duplicate routes
     * by cheapest price.
     * Returns a list with suitable Providers information containing only the lowest
     * price options.
     */

    private List<List<Provider>> getLowestPrice(List<List<Provider>> suitableProvidersByRoute) {
        List<List<Provider>> suitableProvidersWithLowestCost = new ArrayList<>();
        for (List<Provider> providers : suitableProvidersByRoute) {
            List<Provider> tempList = new ArrayList<>();
            for (Provider provider : providers) {
                tempList.add(provider);
                for (int k = 0; k < tempList.size(); k++) {
                    if (tempList.get(k).getRoute_info_uuid().equals(provider.getRoute_info_uuid())) {
                        if (tempList.get(k).getPrice() > provider.getPrice()) {
                            tempList.remove(k);
                        } else if (tempList.get(k).getPrice() < provider.getPrice()) {
                            tempList.removeLast();
                        }
                    }
                }
            }
            suitableProvidersWithLowestCost.add(tempList);
        }
        return suitableProvidersWithLowestCost;
    }

    /**
     * Calculates the total price of all the routes and returns them as a list.
     */

    private List<Long> totalPrice(List<List<Provider>> suitableProvidersWithLowestCost) {
        List<Long> totalPrice = new ArrayList<>();
        for (List<Provider> providers : suitableProvidersWithLowestCost) {
            long tempPrice = 0;
            for (Provider provider : providers) {
                tempPrice = tempPrice + provider.getPrice();
            }
            totalPrice.add(tempPrice);
        }
        return totalPrice;
    }

    /**
     * Prints out all routes filtered by companies, providing the best prices and travel distances
     * along with company names being used.
     */
    private void prettyPrintPaths(List<List<String>> allPaths, String originPlanet,
                                  String destinationPlanet, List<List<Provider>> suitableProvidersWithLowestCost,
                                  List<Long> routeDistance, List<Long> totalPrice) {

        List<String> prettyPrint = new ArrayList<>();
        for (int i = 0; i < allPaths.size(); i++) {
            List<String> allPath = allPaths.get(i);
            List<String> tempList = new ArrayList<>();
            for (String path : allPath) {
                for (Planet planet : planetList) {
                    if (planet.getUuid().equals(path)) {
                        tempList.add(planet.getName());
                        break;
                    }
                }
            }
            prettyPrint.add(String.join(" | ", String.join(" -> ", tempList),
                    String.join("", STR."total lowest cost: \{String.valueOf(totalPrice.get(i))}"),
                    String.join("", STR."total distance: \{String.valueOf(routeDistance.get(i))}")));
        }

        String origin = "";
        String destination = "";
        for (Planet planet : planetList) {
            if (planet.getUuid().equals(originPlanet)) {
                origin = planet.getName();
            } else if (planet.getUuid().equals(destinationPlanet)) {
                destination = planet.getName();
            }
        }

        List<String> selectedCompanies = new ArrayList<>();
        for (List<Provider> providers : suitableProvidersWithLowestCost) {
            for (Provider provider : providers) {
                if (!selectedCompanies.contains(provider.getCompany_uuid())) {
                    selectedCompanies.add(provider.getCompany_uuid());
                }
            }
        }

        System.out.println(STR."All possible routes from \{origin} to \{destination}:");
        for (String path : prettyPrint) {
            System.out.println(path);
        }

        System.out.println("When traveling with companies: ");
        for (String selectedCompany : selectedCompanies) {
            for (Company company : companyList) {
                if (selectedCompany.equals(company.getUuid())) {
                    System.out.println(company.getName());
                }
            }
        }
    }
}



