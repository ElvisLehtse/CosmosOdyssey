package com.cosmos.SQL.postgres;

import com.cosmos.SQL.postgres.initiator.*;

import java.sql.Timestamp;
import java.time.Duration;
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

    public void generateSolutions(String originPlanet, String destinationPlanet, List<String> companiesList) {
        List<String> listOfDefinedCompanyUuid = companySetup(companiesList);
        List<String> listOfDefinedPlanetUuid = planetSetup(originPlanet, destinationPlanet);
        List<String> currentPath = new ArrayList<>();
        List<RouteInfo> routeUuid = new ArrayList<>();
        List<List<RouteInfo>> allPossibleRoutes = findAllPossibleRoutes(listOfDefinedPlanetUuid.getFirst(), listOfDefinedPlanetUuid.getLast(), currentPath, routeUuid, new ArrayList<>(), null);
        List<List<Provider>> suitableProvidersByRoute = suitableProvidersFiltered(listOfDefinedCompanyUuid, allPossibleRoutes);
        Map<Integer, List<List<Provider>>> allPossibleProviderCombinationForAllRoutes = findAllPossibleProviderCombinations(allPossibleRoutes, suitableProvidersByRoute);
        prettyPrintPaths(allPossibleProviderCombinationForAllRoutes, allPossibleRoutes, originPlanet);
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

    private List<List<Provider>> suitableProvidersFiltered(List<String> companyListProvidedByUser, List<List<RouteInfo>> allPossibleRoutes) {
        List<List<Provider>> suitableProvidersByRoute = new ArrayList<>();
        List<Provider> suitableProvidersByCompany = new ArrayList<>();
        for (Provider provider : providerList) {
            if (companyListProvidedByUser.contains(provider.getCompany_uuid())) {
                suitableProvidersByCompany.add(provider);
            }
        }

        Map<String, List<Provider>> groupedProviders = suitableProvidersByCompany.stream()
                .collect(Collectors.groupingBy(Provider::getRoute_info_uuid));

        List<Provider> suitableProviders = new ArrayList<>();

        // CHOICE!
        for (Map.Entry<String, List<Provider>> entry : groupedProviders.entrySet()) {
            List<Provider> providers = entry.getValue();
            providers.sort(Comparator.comparingLong(Provider::getPrice));
            suitableProviders.addAll(providers.stream().limit(3).toList());
        }
        // CHOICE!
        for (Map.Entry<String, List<Provider>> entry : groupedProviders.entrySet()) {
            List<Provider> providers = entry.getValue();
            providers.sort(Comparator.comparingLong(Provider::getTravelTime));
            suitableProviders.addAll(providers.stream().limit(3).toList());
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

        Map<Integer, List<List<Provider>>> allPossibleProvidersTable = new HashMap<>();
        for (int i = 0; i < allPossibleRoutes.size(); i++) {
            List<List<Provider>> masterTempList = new ArrayList<>();
            for (int j = 0; j < allPossibleRoutes.get(i).size(); j++) {
                List<Provider> tempList = new ArrayList<>();
                for (int k = 0; k < suitableProvidersByRoute.get(i).size(); k++) {
                    if (suitableProvidersByRoute.get(i).get(k).getRoute_info_uuid().equals(allPossibleRoutes.get(i).get(j).getUuid())) {
                        tempList.add(suitableProvidersByRoute.get(i).get(k));
                    }
                }
                masterTempList.add(tempList);
            }
            allPossibleProvidersTable.put(i, masterTempList);
        }
        Map<Integer, List<List<Provider>>> allPossibleProviderCombinationForAllRoutes = new HashMap<>();
        for (int i = 0; i < allPossibleProvidersTable.size(); i++) {
            List<List<Provider>> allPossibleProviderCombinations = generateCombinations(allPossibleProvidersTable.get(i));
            allPossibleProviderCombinationForAllRoutes.put(i, allPossibleProviderCombinations);
        }
        return allPossibleProviderCombinationForAllRoutes;
    }

    private List<List<Provider>> generateCombinations(List<List<Provider>> allPossibleProvidersTable) {
        List<List<Provider>> result = new ArrayList<>();
        generateCombinationsRecursive(allPossibleProvidersTable, result, new ArrayList<>(), 0);
        return result;
    }

    private void generateCombinationsRecursive(List<List<Provider>> allPossibleProvidersTable, List<List<Provider>> result, List<Provider> current, int depth) {
        if (depth == allPossibleProvidersTable.size()) {
            result.add(new ArrayList<>(current));
            return;
        }
        for (Provider provider : allPossibleProvidersTable.get(depth)) {
            current.add(provider);
            generateCombinationsRecursive(allPossibleProvidersTable, result, current, depth + 1);
            current.remove(current.size() - 1);
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
     * Calculates the total price of all the routes and returns them as a list.
     */

    private Map<Integer, List<Long>> totalPriceTagForAllProviders(Map<Integer, List<List<Provider>>> allPossibleProviderCombinationForAllRoutes) {
        Map<Integer, List<Long>> totalPriceMap = new HashMap<>();
        for (int i = 0; i < allPossibleProviderCombinationForAllRoutes.size(); i++) {
            List<Long> totalPrice = new ArrayList<>();
            for (int j = 0; j < allPossibleProviderCombinationForAllRoutes.get(i).size(); j++) {
                List<Provider> suitableProviders = allPossibleProviderCombinationForAllRoutes.get(i).get(j);
                long tempPrice = 0;
                for (Provider providers : suitableProviders) {
                    tempPrice = tempPrice + providers.getPrice();
                }
                totalPrice.add(tempPrice);
            }
            totalPriceMap.put(i, totalPrice);
        }
        return totalPriceMap;
    }

    private Map<Integer, List<String>> totalTravelTimeForAllProviders(Map<Integer, List<List<Provider>>> allPossibleProviderCombinationForAllRoutes) {
        Map<Integer, List<String>> travelTimeForAll = new HashMap<>();
        for (int i = 0; i < allPossibleProviderCombinationForAllRoutes.size(); i++) {
            travelTimeForAll.put(i, totalTravelTimeForSpecificProviders(allPossibleProviderCombinationForAllRoutes.get(i)));
        }
        return travelTimeForAll;
    }

    private List<String> totalTravelTimeForSpecificProviders(List<List<Provider>> suitableProviders) {
        int hoursInDay = 24;
        List<String> travelTime = new ArrayList<>();
        for (int i = 0; i < suitableProviders.size(); i++) {
            long tempHours;
            long hours = 0;
            for (int j = 0; j < suitableProviders.get(i).size(); j++) {
                tempHours = Duration.between(suitableProviders.get(i).get(j).getFlight_start().toLocalDateTime(), suitableProviders.get(i).get(j).getFlight_end().toLocalDateTime()).toHours();
                hours = hours + tempHours;
            }
            long days = hours / hoursInDay;
            hours = hours - (days * hoursInDay);
            String travelTimeText = STR."\{days}d \{hours}h";
            travelTime.add(travelTimeText);
        }
        return travelTime;
    }

    private List<List<String>> companiesUsedForTravel(Map<Integer, List<List<Provider>>> allPossibleProviderCombinationForAllRoutes) {
        List<List<String>> companies = new ArrayList<>();
        for (int i = 0; i < allPossibleProviderCombinationForAllRoutes.size(); i++) {
            List<List<Provider>> suitableProviders = allPossibleProviderCombinationForAllRoutes.get(i);
            List <String> selectedCompanies = new ArrayList<>();
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
            companies.add(selectedCompanies);
        }
        return companies;
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
        Long price;
        String travelTime;
        String routeDistance;
        String path;
        String company;

        public Test(long price, String travelTime, String routeDistance, String path, String company) {
            this.price = price;
            this.travelTime = travelTime;
            this.routeDistance = routeDistance;
            this.path = path;
            this.company = company;
        }
    }

    /**
     * Prints out all routes filtered by companies, providing the best prices and travel distances
     * along with company names being used.
     */
    private void prettyPrintPaths(Map<Integer, List<List<Provider>>> allPossibleProviderCombinationForAllRoutes, List<List<RouteInfo>> allPossibleRoutes, String originPlanet) {
        List<Long> totalDistance = routesDistance(allPossibleRoutes);
        Map<Integer, List<String>> totalTravelTime = totalTravelTimeForAllProviders(allPossibleProviderCombinationForAllRoutes);
        Map<Integer, List<Long>> totalPrice = totalPriceTagForAllProviders(allPossibleProviderCombinationForAllRoutes);
        List<List<String>> companies = companiesUsedForTravel(allPossibleProviderCombinationForAllRoutes);
        List<Test> test = new ArrayList<>();
        for (int i = 0; i < allPossibleProviderCombinationForAllRoutes.size(); i++) {
            for (int j = 0; j < allPossibleProviderCombinationForAllRoutes.get(i).size(); j++) {
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
                totalTravelTime.get(i).get(j);
                test.add(new Test(totalPrice.get(i).get(j), totalTravelTime.get(i).get(j), String.valueOf(totalDistance.get(i)), String.join(" -> ", planetName), companies.get(i).get(j)));
            }
        }
        List<Test> testSorted = test.stream().sorted((o1, o2) -> o1.price.compareTo(o2.price)).toList();
        for (int i = 0; i < testSorted.size(); i++) {
            System.out.println(testSorted.get(i).path);
            System.out.println("Cost: " + testSorted.get(i).price + " | travel time " + test.get(i).travelTime + " | distance " + test.get(i).routeDistance);
            System.out.println(testSorted.get(i).company);
        }
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







    //--------- GRAVEYARD ------------


    /*
    /**
     * Takes in the suitable Provider list and filters out duplicate routes
     * by cheapest price.
     * Returns a list with suitable Providers information containing only the lowest
     * price options.
     */
/*
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

     */


}



