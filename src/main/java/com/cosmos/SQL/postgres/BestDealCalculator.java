package com.cosmos.SQL.postgres;

import com.cosmos.SQL.postgres.initiator.*;
import com.cosmos.server.RequestHandler;

import java.sql.Timestamp;
import java.util.*;

public class BestDealCalculator {

    public BestDealCalculator () {
        InitiateLists initiateLists = new InitiateLists();
        routeList = initiateLists.getRouteList();
        planetList = initiateLists.getPlanetList();
        providerList = initiateLists.getProviderList();
        companyList = initiateLists.getCompanyList();
    }
    static List<RouteInfo> routeList = new ArrayList<>();
    static List<Planet> planetList = new ArrayList<>();
    static List<Provider> providerList = new ArrayList<>();
    static List<Company> companyList = new ArrayList<>();

    private List<String> planetSetup(String originPlanet, String destinationPlanet) {
        List<String> userDefinedPlanetNames = new ArrayList<>();
        userDefinedPlanetNames.add(originPlanet);
        userDefinedPlanetNames.add(destinationPlanet);
        List<String> listOfDefinedPlanetUuid = new ArrayList<>();
        for (String selectedPlanets : userDefinedPlanetNames) {
            for (Planet planet : planetList) {
                if (selectedPlanets.equals(planet.getName())) {
                    listOfDefinedPlanetUuid.add(planet.getUuid());
                }
            }
        }
        return listOfDefinedPlanetUuid;
    }

    private List<String> companySetup(List<String> userDefinedCompanyNames) {
        List<String> listOfDefinedCompanyUuid = new ArrayList<>();
        for (String selectedCompanies : userDefinedCompanyNames) {
            for (Company company : companyList) {
                if (selectedCompanies.equals(company.getName())) {
                    listOfDefinedCompanyUuid.add(company.getUuid());
                }
            }
        }
        return listOfDefinedCompanyUuid;
    }

    public void generateSolutions(String originPlanet, String destinationPlanet, List<String> companiesList) {
        List<String> listOfDefinedCompanyUuid = companySetup(companiesList);
        List<String> listOfDefinedPlanetUuid = planetSetup(originPlanet, destinationPlanet);
        List<String> currentPath = new ArrayList<>();
        List<RouteInfo> routeUuid = new ArrayList<>();
        List<List<RouteInfo>> allPossibleRoutes = findAllPossibleRoutes(listOfDefinedPlanetUuid.getFirst(), listOfDefinedPlanetUuid.getLast(), currentPath, routeUuid, new ArrayList<>(), null);
        List<List<Provider>> suitableProvidersByRoute = providersFilteredByCompanies(listOfDefinedCompanyUuid, allPossibleRoutes);
        List<List<Provider>> suitableProvidersWithLowestCost = getLowestPrice(suitableProvidersByRoute);
        List<List<Provider>> suitableProvidersWithFastestTravel = getFastestTravelTime(suitableProvidersByRoute);
        List<Long> routeDistance = routesDistance(allPossibleRoutes);
        Map<Integer, List<List<Provider>>> allPossibleProviders = findAllPossibleProviderCombinations(allPossibleRoutes, suitableProvidersByRoute);
        prettyPrintPaths(suitableProvidersWithLowestCost, suitableProvidersWithFastestTravel, routeDistance, allPossibleProviders, allPossibleRoutes, originPlanet);
    }

    public void generateUnfilteredSolutions(String originPlanet, String destinationPlanet) {
        List<String> allCompaniesList = new ArrayList<>();
        for (Company company : companyList) {
            allCompaniesList.add(company.getName());
        }
        generateSolutions(originPlanet, destinationPlanet, allCompaniesList);
    }

    /**
     * Finds all possible routes from origin planet to destination planet.
     * Routes are returned as lists within a list, containing route_info UUID-s.
     */

    private List<List<RouteInfo>> findAllPossibleRoutes(String originPlanet, String destinationPlanet, List<String> currentPath,
                                                     List<RouteInfo> listOfRoutes, List<List<RouteInfo>> allPossibleRoutes, RouteInfo routeInfo) {
        currentPath.add(originPlanet);
        if (routeInfo != null) {
            listOfRoutes.add(routeInfo);
        }
        if (originPlanet.equals(destinationPlanet)) {
            allPossibleRoutes.add(listOfRoutes);
        }
        for (RouteInfo route : routeList) {
            if (route.getOriginPlanetUuid().equals(originPlanet)) {
                if (!currentPath.contains(route.getDestinationPlanetUuid())) {
                    findAllPossibleRoutes(route.getDestinationPlanetUuid(), destinationPlanet, new ArrayList<>(currentPath),
                            new ArrayList<>(listOfRoutes), allPossibleRoutes, route);
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

    private List<List<Provider>> providersFilteredByCompanies(List<String> companyListProvidedByUser, List<List<RouteInfo>> allPossibleRoutes) {
        List<List<Provider>> suitableProvidersByRoute = new ArrayList<>();
        List<Provider> suitableProviders = new ArrayList<>();
        for (Provider provider : providerList) {
            if (companyListProvidedByUser.contains(provider.getCompany_uuid())) {
                suitableProviders.add(provider);
            }
        }
        for (List<RouteInfo> allPossibleRoute : allPossibleRoutes) {
            List<Provider> tempListForSuitableProvidersByRoute = new ArrayList<>();
            for (RouteInfo routeInfo : allPossibleRoute) {
                for (Provider suitableProvider : suitableProviders) {
                    if (routeInfo.getUuid().equals(suitableProvider.getRoute_info_uuid())) {
                        tempListForSuitableProvidersByRoute.add(suitableProvider);
                    }
                }
            }
            suitableProvidersByRoute.add(tempListForSuitableProvidersByRoute);
        }
        return suitableProvidersByRoute;
    }

    private Map<Integer, List<List<Provider>>> findAllPossibleProviderCombinations(List<List<RouteInfo>> allPossibleRoutes, List<List<Provider>> suitableProvidersByRoute) {
        Map<Integer, List<List<Provider>>> allPossibleProviders = new HashMap<>();
        Map<String, Set<Provider>> routeMap = new HashMap<>();
        for (int i = 0; i < allPossibleRoutes.size(); i++) {
            for (Provider provider : suitableProvidersByRoute.get(i)) {
                routeMap.computeIfAbsent(provider.getRoute_info_uuid(), index -> new HashSet<>()).add(provider);
            }
            Map<String, Set<Provider>> routeMapFiltered = new HashMap<>();
            for (int j = 0; j < allPossibleRoutes.get(i).size(); j++) {
                routeMapFiltered.put(allPossibleRoutes.get(i).get(j).getUuid(), routeMap.get(allPossibleRoutes.get(i).get(j).getUuid()));
            }
            List<List<Provider>> listOfProviders = new LinkedList<>();
            recurse(routeMapFiltered, new LinkedList<>(routeMapFiltered.keySet()).listIterator(), new HashMap<>(), listOfProviders);
            allPossibleProviders.put(i, listOfProviders);
        }
        return allPossibleProviders;
    }

    private void recurse(Map<String,Set<Provider>> map, ListIterator<String> listIterator, Map<String, Provider> cur, List<List<Provider>> listOfProviders) {
        if( !listIterator.hasNext() ) {
            List<Provider> tempList = new ArrayList<>();
            for( String key : cur.keySet() ) {
                tempList.add(cur.get(key));
            }
            listOfProviders.add(tempList);
        } else {
            String key = listIterator.next();
            Set<Provider> set = map.get(key);

            for( Provider value : set ) {
                cur.put(key, value);
                recurse(map, listIterator, cur, listOfProviders);
                cur.remove(key);
            }
            listIterator.previous();
        }
    }

    /**
     * Calculates the total distances for all possible routes.
     * Route distances are returned as a list.
     */

    private static List<Long> routesDistance(List<List<RouteInfo>> allPossibleRoutes) {
        List<Long> routeDistance = new ArrayList<>();
        for (List<RouteInfo> allPossibleRoute : allPossibleRoutes) {
            long tempList = 0L;
            for (RouteInfo routeInfo : allPossibleRoute) {
                tempList = tempList + routeInfo.getDistance();
            }
            routeDistance.add(tempList);
        }
        return routeDistance;
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

    private List<List<Provider>> getFastestTravelTime(List<List<Provider>> suitableProvidersByRoute) {
        List<List<Provider>> suitableProvidersWithFastestTravel = new ArrayList<>();
        for (List<Provider> providers : suitableProvidersByRoute) {
            List<Provider> tempList = new ArrayList<>();
            for (Provider provider : providers) {
                tempList.add(provider);
                for (int k = 0; k < tempList.size(); k++) {
                    if (tempList.get(k).getRoute_info_uuid().equals(provider.getRoute_info_uuid())) {
                        Timestamp tempListStart = Timestamp.valueOf(tempList.get(k).getFlight_start().toString().replace("+03", ""));
                        Timestamp tempListEnd = Timestamp.valueOf(tempList.get(k).getFlight_end().toString().replace("+03", ""));
                        double tempListDiff = tempListEnd.getTime() - tempListStart.getTime();
                        Timestamp providerStart = Timestamp.valueOf(provider.getFlight_start().toString().replace("+03", ""));
                        Timestamp providerEnd = Timestamp.valueOf(provider.getFlight_end().toString().replace("+03", ""));
                        double providerDiff = providerEnd.getTime() - providerStart.getTime();
                        if (tempListDiff > providerDiff) {
                            tempList.remove(k);
                        } else if (tempListDiff < providerDiff) {
                            tempList.removeLast();
                        }
                    }
                }
            }
            suitableProvidersWithFastestTravel.add(tempList);
        }
        return suitableProvidersWithFastestTravel;
    }

    /**
     * Calculates the total price of all the routes and returns them as a list.
     */

    private Map<Integer, List<Long>> totalPriceTagForAllProviders(Map<Integer, List<List<Provider>>> allSuitableProviders) {
        Map<Integer, List<Long>> totalPriceMap = new HashMap<>();
        for (int i = 0; i < allSuitableProviders.size(); i++) {
            List<List<Provider>> suitableProviders = allSuitableProviders.get(i);
            List<Long> totalPrice = totalPriceTagForSpecificProviders(suitableProviders);
            totalPriceMap.put(i, totalPrice);
        }
        return totalPriceMap;
    }

    private List<Long> totalPriceTagForSpecificProviders(List<List<Provider>> suitableProviders) {
        List<Long> totalPrice = new ArrayList<>();
        for (List<Provider> providers : suitableProviders) {
            long tempPrice = 0;
            for (Provider provider : providers) {
                tempPrice = tempPrice + provider.getPrice();
            }
            totalPrice.add(tempPrice);
        }
        return totalPrice;
    }

    private Map<Integer, List<String>> totalTravelTimeForAllProviders(Map<Integer, List<List<Provider>>> allPossibleProviders) {
        Map<Integer, List<String>> travelTimeForAll = new HashMap<>();
        int maximumLimitPerRoute = 20;
        for (int i = 0; i < allPossibleProviders.size(); i++) {
            List<String> tempList = new ArrayList<>();
            for (int j = 0; j < allPossibleProviders.get(i).size() && j < maximumLimitPerRoute; j++) {
                tempList.add(totalTravelTimeForSpecificProviders(allPossibleProviders.get(i)).get(j));
            }
            travelTimeForAll.put(i, tempList);
        }
        return travelTimeForAll;
    }


    private List<String> totalTravelTimeForSpecificProviders(List<List<Provider>> suitableProviders) {
        int millisecondsInHour = 3600000;
        int hoursInDay = 24;
        List<String> travelTime = new ArrayList<>();
        for (List<Provider> providers : suitableProviders) {
            double tempTravelTime = 0d;
            for (Provider provider : providers) {
                Timestamp providerStart = Timestamp.valueOf(provider.getFlight_start().toString().replace("+03", ""));
                Timestamp providerEnd = Timestamp.valueOf(provider.getFlight_end().toString().replace("+03", ""));
                double providerDiff = providerEnd.getTime() - providerStart.getTime();
                tempTravelTime = tempTravelTime + providerDiff;
            }
            double hours = tempTravelTime / millisecondsInHour;
            int days = 0;
            while (hours > hoursInDay) {
                hours = (double) Math.round((hours - hoursInDay)*10)/10;
                days++;
            }
            String travelTimeText = STR."\{days}d \{hours}h";
            travelTime.add(travelTimeText);
        }
        return travelTime;
    }

    private String companiesUsedForTravel(List<List<Provider>> suitableProviders) {
        List<String> selectedCompanies = new ArrayList<>();
        for (List<Provider> suitableProvider : suitableProviders) {
            List<String> companyNames = new ArrayList<>();
            for (Provider provider : suitableProvider) {
                for (Company company : companyList) {
                    if (provider.getCompany_uuid().equals(company.getUuid())) {
                        companyNames.add(company.getName());
                    }
                }
            }
            selectedCompanies.add(String.join(" -> ", companyNames));
        }
        StringBuilder stringBuilder = new StringBuilder();
        for (String selectedCompany : selectedCompanies) {
            stringBuilder.append(STR."\{selectedCompany}<br>");
        }
        return stringBuilder.toString();
    }

    private String pathSetter(List<String> prettyPrint, String value) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < prettyPrint.size(); i++) {
            stringBuilder.append("<br><form actions=\"\" method=\"post\">");
            stringBuilder.append(STR."<button name=\"routeName\" type=\"submit\" value=\"\{value}\{i+1}\">Reserve route nr \{i+1}</button>");
            stringBuilder.append(STR." \{prettyPrint.get(i)}");
            stringBuilder.append("</form><br>");
        }
        return stringBuilder.toString();
    }

    static class Test{
        long price;
        String travelTime;
        String routeDistance;

        public Test(long price, String travelTime, String routeDistance) {
            this.price = price;
            this.travelTime = travelTime;
            this.routeDistance = routeDistance;
        }
    }

    /**
     * Prints out all routes filtered by companies, providing the best prices and travel distances
     * along with company names being used.
     */
    private void prettyPrintPaths(List<List<Provider>> suitableProvidersWithLowestCost,
                                  List<List<Provider>> suitableProvidersWithFastestTravel, List<Long> routeDistance, Map<Integer,
            List<List<Provider>>> allPossibleProviders, List<List<RouteInfo>> allPossibleRoutes, String originPlanet) {
        int maximumLimitPerRoute = 20;
        List<Long> priceForLowestCost = totalPriceTagForSpecificProviders(suitableProvidersWithLowestCost);
        List<Long> priceForFastestTravel = totalPriceTagForSpecificProviders(suitableProvidersWithFastestTravel);
        List<String> travelTimeForLowestCost = totalTravelTimeForSpecificProviders(suitableProvidersWithLowestCost);
        List<String> travelTimeForFastestTravel = totalTravelTimeForSpecificProviders(suitableProvidersWithFastestTravel);
        Map<Integer, List<String>> travelTimeForAll = totalTravelTimeForAllProviders(allPossibleProviders);
        Map<Integer, List<Long>> priceForAllCost = totalPriceTagForAllProviders(allPossibleProviders);

        List<String> prettyPrintCheapest = new ArrayList<>();
        List<String> prettyPrintFastest = new ArrayList<>();
        for (int i = 0; i < allPossibleRoutes.size(); i++) {
            List<RouteInfo> allPath = allPossibleRoutes.get(i);
            List<String> planetName = new ArrayList<>();
            planetName.add(originPlanet);
            for (RouteInfo path : allPath) {
                for (Planet planet : planetList) {
                    if (planet.getUuid().equals(path.getDestinationPlanetUuid())) {
                        planetName.add(planet.getName());
                        break;
                    }
                }
            }
            prettyPrintCheapest.add(String.join(" | ", String.join(" -> ", planetName),
                    String.join("", STR."total cost: \{String.valueOf(priceForLowestCost.get(i))}"), String.join("", STR."total travel time: \{travelTimeForLowestCost.get(i)}"),
                    String.join("", STR."total distance: \{String.valueOf(routeDistance.get(i))}")));

            prettyPrintFastest.add(String.join(" | ", String.join(" -> ", planetName),
                    String.join("", STR."total cost: \{String.valueOf(priceForFastestTravel.get(i))}"), String.join("", STR."total travel time: \{travelTimeForFastestTravel.get(i)}"),
                    String.join("", STR."total distance: \{String.valueOf(routeDistance.get(i))}")));
        }
        List<Test> test = new ArrayList<>();
        List<String> prettyPrintAll = new ArrayList<>();
        for (int i = 0; i < allPossibleProviders.size(); i++) {
            for (int j = 0; j < allPossibleProviders.get(i).size() && j < maximumLimitPerRoute; j++) {
                List<RouteInfo> allPath = allPossibleRoutes.get(i);
                List<String> planetName = new ArrayList<>();
                planetName.add(originPlanet);
                for (RouteInfo path : allPath) {
                    for (Planet planet : planetList) {
                        if (planet.getUuid().equals(path.getDestinationPlanetUuid())) {
                            planetName.add(planet.getName());
                            break;
                        }
                    }
                }
                test.add(new Test(priceForAllCost.get(i).get(j), travelTimeForAll.get(i).get(j), String.valueOf(routeDistance.get(i))));
                prettyPrintAll.add(String.join(" | ", String.join(" -> ", planetName),
                        String.join("", STR."total cost: \{String.valueOf(priceForAllCost.get(i).get(j))}"), String.join("", STR."total travel time: \{travelTimeForAll.get(i).get(j)}"),
                        String.join("", STR."total distance: \{String.valueOf(routeDistance.get(i))}")));
            }
        }

        for (int i = 0; i < test.size(); i++) {
            System.out.println(test.get(i).price + " " + test.get(i).travelTime + " " + test.get(i).routeDistance);
        }

        RequestHandler.setCheapestCompanies(companiesUsedForTravel(suitableProvidersWithLowestCost));
        RequestHandler.setFastestCompanies(companiesUsedForTravel(suitableProvidersWithLowestCost));
        RequestHandler.setAllPath(pathSetter(prettyPrintAll, "all"));
        RequestHandler.setCheapestPath(pathSetter(prettyPrintCheapest, "cheapest"));
        RequestHandler.setFastestPath(pathSetter(prettyPrintFastest, "fastest"));
    }
/*
    private void storeUserChoice(List<List<Provider>> suitableProvidersWithLowestCost, int userRouteChoice, String userFirstName, String userLastName) {
        String reservation = "INSERT INTO reservation(" +
                "uuid, first_name, last_name)" +
                "VALUES (?, ?, ?)";
        UUID reservationUuid = UUID.randomUUID();
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(reservation);
            preparedStatement.setObject(1, reservationUuid);
            preparedStatement.setString(2, userFirstName);
            preparedStatement.setString(3, userLastName);
            preparedStatement.execute();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        String reservedRoutes = "INSERT INTO reserved_routes(" +
                "uuid, reservation_uuid, provider_uuid)" +
                "VALUES (?, ?, ?)";
        for (int i = 0; i < suitableProvidersWithLowestCost.get(userRouteChoice - 1).size(); i++) {
            try {
                UUID reservedRoutesUuid = UUID.randomUUID();
                PreparedStatement preparedStatement = connection.prepareStatement(reservedRoutes);
                preparedStatement.setObject(1, reservedRoutesUuid);
                preparedStatement.setObject(2, reservationUuid);
                preparedStatement.setObject(3, UUID.fromString(suitableProvidersWithLowestCost.get(userRouteChoice - 1).get(i).getUuid()));
                preparedStatement.execute();
            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }
        }
    }*/
}



