package com.cosmos.SQL.postgres;

import com.cosmos.SQL.postgres.initiator.*;
import com.cosmos.server.RequestHandler;

import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

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

    public void generateFilteredSolutions(String originPlanet, String destinationPlanet, List<String> companyListProvidedByUser) {
        List<String> listOfDefinedCompanyUuid = companySetup(companyListProvidedByUser);
        List<String> listOfDefinedPlanetUuid = planetSetup(originPlanet, destinationPlanet);
        List<String> currentPath = new ArrayList<>();
        List<String> routeUuid = new ArrayList<>();
        List<List<String>> allPossibleRoutes = findAllPossibleRoutes(listOfDefinedPlanetUuid.getFirst(), listOfDefinedPlanetUuid.getLast(), routeUuid, currentPath, new ArrayList<>(), null);
        List<List<Provider>> suitableProvidersByRoute = filterByCompany(listOfDefinedCompanyUuid, allPossibleRoutes);
        List<Long> routeDistance = routesDistance(allPossibleRoutes);
        List<List<String>> allPaths = getPaths(listOfDefinedPlanetUuid.getFirst(), allPossibleRoutes);
        List<List<Provider>> suitableProvidersWithLowestCost = getLowestPrice(suitableProvidersByRoute);
        List<List<Provider>> suitableProvidersWithFastestTravel = getFastestTravelTime(suitableProvidersByRoute);

        prettyPrintPaths(allPaths, suitableProvidersWithLowestCost, suitableProvidersWithFastestTravel, routeDistance);
    }

    public void generateSolutions(String originPlanet, String destinationPlanet) {
        List<String> listOfDefinedPlanetUuid = planetSetup(originPlanet, destinationPlanet);
        List<String> currentPath = new ArrayList<>();
        List<String> routeUuid = new ArrayList<>();
        List<List<String>> allPossibleRoutes = findAllPossibleRoutes(listOfDefinedPlanetUuid.getFirst(), listOfDefinedPlanetUuid.getLast(), routeUuid, currentPath, new ArrayList<>(), null);

        Map<Integer, List<List<String>>> allPossibleProviders = findAllPossibleProviders(allPossibleRoutes);

        List<Long> routeDistance = routesDistance(allPossibleRoutes);
        List<List<String>> allPaths = getPaths(listOfDefinedPlanetUuid.getFirst(), allPossibleRoutes);
        List<String> allCompaniesList = new ArrayList<>();
        for (Company company : companyList) {
            allCompaniesList.add(company.getUuid());
        }
        List<List<Provider>> suitableProvidersByRoute = filterByCompany(allCompaniesList, allPossibleRoutes);
        List<List<Provider>> suitableProvidersWithLowestCost = getLowestPrice(suitableProvidersByRoute);
        List<List<Provider>> suitableProvidersWithFastestTravel = getFastestTravelTime(suitableProvidersByRoute);

        prettyPrintPaths(allPaths, suitableProvidersWithLowestCost, suitableProvidersWithFastestTravel, routeDistance);
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

    private Map<Integer, List<List<String>>> findAllPossibleProviders(List<List<String>> allPossibleRoutes) {
        Map<Integer, List<List<String>>> allPossibleProviders = new HashMap<>();

        Map<String, Set<String>> routeMap = new HashMap<>();
        for (Provider provider : providerList) {
            routeMap.computeIfAbsent(provider.getRoute_info_uuid(), index -> new HashSet<>()).add(provider.getUuid());
        }
        for (int i = 0; i < allPossibleRoutes.size(); i++) {
            Map<String, Set<String>> routeMapFiltered = new HashMap<>();
            for (int j = 0; j < allPossibleRoutes.get(i).size(); j++) {
                routeMapFiltered.put(allPossibleRoutes.get(i).get(j), routeMap.get(allPossibleRoutes.get(i).get(j)));
            }
            List<List<String>> list = new LinkedList<>();
            recurse(routeMapFiltered, new LinkedList<>(routeMapFiltered.keySet()).listIterator(), new HashMap<>(), list);
            allPossibleProviders.put(i, list);
        }

        /*
        for (int i = 0; i < allPossibleProviders.size(); i++) {
            for (int j = 0; j < allPossibleProviders.get(i).size(); j++) {
                System.out.println(allPossibleProviders.get(i).get(j));
            }
            System.out.println("\n");
        }
         */
         return allPossibleProviders;
    }

    private void recurse(Map<String,Set<String>> map, ListIterator<String> listIterator, Map<String,String> cur, List<List<String>> list ) {
        if( !listIterator.hasNext() ) {
            List<String> tempList = new ArrayList<>();
            for( String key : cur.keySet() ) {
                tempList.add(cur.get(key));
            }
            list.add(tempList);
        } else {
            String key = listIterator.next();
            Set<String> set = map.get(key);

            for( String value : set ) {
                cur.put(key, value);
                recurse(map, listIterator, cur, list);
                cur.remove(key);
            }
            listIterator.previous();
        }
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
                        tempListForSuitableProvidersByRoute.add(suitableProvider);
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

    private List<Long> totalPriceTag(List<List<Provider>> suitableProviders) {
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

    private List<String> totalTravelTime(List<List<Provider>> suitableProviders) {
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

    /**
     * Prints out all routes filtered by companies, providing the best prices and travel distances
     * along with company names being used.
     */
    private void prettyPrintPaths(List<List<String>> allPaths, List<List<Provider>> suitableProvidersWithLowestCost,
                                  List<List<Provider>> suitableProvidersWithFastestTravel, List<Long> routeDistance) {

        List<Long> priceForLowestCost = totalPriceTag(suitableProvidersWithLowestCost);
        List<Long> priceForFastestTravel = totalPriceTag(suitableProvidersWithFastestTravel);
        List<String> travelTimeForLowestCost = totalTravelTime(suitableProvidersWithLowestCost);
        List<String> travelTimeForFastestTravel = totalTravelTime(suitableProvidersWithFastestTravel);

        List<String> prettyPrintCheapest = new ArrayList<>();
        List<String> prettyPrintFastest = new ArrayList<>();
        for (int i = 0; i < allPaths.size(); i++) {
            List<String> allPath = allPaths.get(i);
            List<String> planetName = new ArrayList<>();
            for (String path : allPath) {
                for (Planet planet : planetList) {
                    if (planet.getUuid().equals(path)) {
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

        List<String> selectedCompaniesWithLowestCost = new ArrayList<>();
        for (List<Provider> providers : suitableProvidersWithLowestCost) {
            for (Provider provider : providers) {
                if (!selectedCompaniesWithLowestCost.contains(provider.getCompany_uuid())) {
                    selectedCompaniesWithLowestCost.add(provider.getCompany_uuid());
                }
            }
        }

        List<String> selectedCompaniesFastestTravelTime = new ArrayList<>();
        for (List<Provider> providers : suitableProvidersWithFastestTravel) {
            for (Provider provider : providers) {
                if (!selectedCompaniesFastestTravelTime.contains(provider.getCompany_uuid())) {
                    selectedCompaniesFastestTravelTime.add(provider.getCompany_uuid());
                }
            }
        }

        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < prettyPrintCheapest.size(); i++) {
            stringBuilder.append("<br><form actions=\"\" method=\"post\">");
            stringBuilder.append(STR."<button name=\"routeName\" type=\"submit\" value=\"cheapest\{i+1}\">Reserve route nr \{i+1}</button>");
            stringBuilder.append(STR." \{prettyPrintCheapest.get(i)}");
            stringBuilder.append("</form><br>");
        }
        RequestHandler.setCheapestPath(stringBuilder.toString());

        stringBuilder = new StringBuilder();
        for (int i = 0; i < prettyPrintFastest.size(); i++) {
            stringBuilder.append("<br><form actions=\"\" method=\"post\">");
            stringBuilder.append(STR."<button name=\"routeName\" type=\"submit\" value=\"fastest\{i+1}\">Reserve route nr \{i+1}</button>");
            stringBuilder.append(STR." \{prettyPrintFastest.get(i)}");
            stringBuilder.append("</form><br>");
        }
        RequestHandler.setFastestPath(stringBuilder.toString());

        stringBuilder = new StringBuilder();
        for (String selectedCompany : selectedCompaniesWithLowestCost) {
            for (Company company : companyList) {
                if (selectedCompany.equals(company.getUuid())) {
                    stringBuilder.append(company.getName());
                    stringBuilder.append("<br>");
                }
            }
        }
        RequestHandler.setCheapestCompanies(stringBuilder.toString());

        stringBuilder = new StringBuilder();
        for (String selectedCompany : selectedCompaniesFastestTravelTime) {
            for (Company company : companyList) {
                if (selectedCompany.equals(company.getUuid())) {
                    stringBuilder.append(company.getName());
                    stringBuilder.append("<br>");
                }
            }
        }
        RequestHandler.setFastestCompanies(stringBuilder.toString());
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



