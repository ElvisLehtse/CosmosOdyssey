package com.cosmos.SQL.postgres;

import com.cosmos.SQL.postgres.initiator.*;
import com.cosmos.server.RequestHandler;

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

    public void generateSolutions(String originPlanet, String destinationPlanet, List<String> companiesList, String filterBy) {
        List<String> listOfDefinedCompanyUuid = companySetup(companiesList);
        List<String> listOfDefinedPlanetUuid = planetSetup(originPlanet, destinationPlanet);
        List<String> currentPath = new ArrayList<>();
        List<RouteInfo> routeUuid = new ArrayList<>();
        List<List<RouteInfo>> allPossibleRoutes = findAllPossibleRoutes(listOfDefinedPlanetUuid.getFirst(), listOfDefinedPlanetUuid.getLast(), currentPath, routeUuid, new ArrayList<>(), null);
        List<List<Provider>> suitableProvidersByRoute = suitableProvidersFiltered(listOfDefinedCompanyUuid, allPossibleRoutes, filterBy);
        Map<Integer, List<List<Provider>>> allPossibleProviderCombinationForAllRoutes = findAllPossibleProviderCombinations(allPossibleRoutes, suitableProvidersByRoute);
        prettyPrintPaths(allPossibleProviderCombinationForAllRoutes, allPossibleRoutes, originPlanet, filterBy);
    }

    public void generateUnfilteredSolutions(String originPlanet, String destinationPlanet, String filterBy) {
        List<String> allCompaniesList = new ArrayList<>();
        for (Company company : companyList) {
            allCompaniesList.add(company.getName());
        }
        generateSolutions(originPlanet, destinationPlanet, allCompaniesList, filterBy);
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

    private List<List<Provider>> suitableProvidersFiltered(List<String> companyListProvidedByUser, List<List<RouteInfo>> allPossibleRoutes, String filterBy) {
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

        if (filterBy.equals("price")) {
            for (Map.Entry<String, List<Provider>> entry : groupedProviders.entrySet()) {
                List<Provider> providers = entry.getValue();
                providers.sort(Comparator.comparingLong(Provider::getPrice));
                suitableProviders.addAll(providers.stream().limit(3).toList());
            }
        } else if (filterBy.equals("time")) {
            for (Map.Entry<String, List<Provider>> entry : groupedProviders.entrySet()) {
                List<Provider> providers = entry.getValue();
                providers.sort(Comparator.comparingLong(Provider::getTravelTime));
                suitableProviders.addAll(providers.stream().limit(3).toList());
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

    private Map<Integer, List<Long>> totalTravelTimeForAllProviders(Map<Integer, List<List<Provider>>> allPossibleProviderCombinationForAllRoutes) {
        Map<Integer, List<Long>> travelTimeForAll = new HashMap<>();
        for (int i = 0; i < allPossibleProviderCombinationForAllRoutes.size(); i++) {
            travelTimeForAll.put(i, totalTravelTimeForSpecificProviders(allPossibleProviderCombinationForAllRoutes.get(i)));
        }
        return travelTimeForAll;
    }

    private List<Long> totalTravelTimeForSpecificProviders(List<List<Provider>> suitableProviders) {
        List<Long> travelTime = new ArrayList<>();
        for (int i = 0; i < suitableProviders.size(); i++) {
            long tempHours;
            long hours = 0;
            for (int j = 0; j < suitableProviders.get(i).size(); j++) {
                tempHours = Duration.between(suitableProviders.get(i).get(j).getFlight_start().toLocalDateTime(), suitableProviders.get(i).get(j).getFlight_end().toLocalDateTime()).toHours();
                hours = hours + tempHours;
            }
            travelTime.add(hours);
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

    static class Test{
        public Long getPrice() {
            return price;
        }

        public String getTravelTime() {
            return travelTime;
        }

        public Long getDistance() {
            return distance;
        }

        public String getPath() {
            return path;
        }

        public String getCompany() {
            return company;
        }

        Long price;
        String travelTime;
        Long distance;
        String path;
        String company;

        public Test(long price, String travelTime, Long distance, String path, String company) {
            this.price = price;
            this.travelTime = travelTime;
            this.distance = distance;
            this.path = path;
            this.company = company;
        }
    }

    private String travelTimeToString(Long totalTravelTime) {
        int hoursInDay = 24;
        long hours = totalTravelTime;
        long days = hours / hoursInDay;
        hours = hours - (days * hoursInDay);
        return STR."\{days}d \{hours}h";
    }

    /**
     * Prints out all routes filtered by companies, providing the best prices and travel distances
     * along with company names being used.
     */
    private void prettyPrintPaths(Map<Integer, List<List<Provider>>> allPossibleProviderCombinationForAllRoutes, List<List<RouteInfo>> allPossibleRoutes, String originPlanet, String filterBy) {
        int showMaxNumberOfCombinationsPerRoute = 3;
        List<Long> totalDistance = routesDistance(allPossibleRoutes);
        Map<Integer, List<Long>> totalTravelTime = totalTravelTimeForAllProviders(allPossibleProviderCombinationForAllRoutes);
        Map<Integer, List<Long>> totalPrice = totalPriceTagForAllProviders(allPossibleProviderCombinationForAllRoutes);
        List<List<String>> companies = companiesUsedForTravel(allPossibleProviderCombinationForAllRoutes);
        List<Test> test = new ArrayList<>();
        List<List<Provider>> listForGettingUserChoice = new ArrayList<>();
        for (int i = 0; i < allPossibleProviderCombinationForAllRoutes.size(); i++) {
            for (int j = 0; j < allPossibleProviderCombinationForAllRoutes.get(i).size() && j < showMaxNumberOfCombinationsPerRoute; j++) {
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
                test.add(new Test(totalPrice.get(i).get(j), travelTimeToString(totalTravelTime.get(i).get(j)), totalDistance.get(i), String.join(" -> ", planetName), companies.get(i).get(j)));
                listForGettingUserChoice.add(allPossibleProviderCombinationForAllRoutes.get(i).get(j));
            }
        }
        List<Test> testSorted = switch (filterBy) {
            case "price" -> test.stream().sorted(Comparator.comparing(Test::getDistance).thenComparing(Test::getPrice)).toList();
            case "time" -> test.stream().sorted(Comparator.comparing(Test::getDistance)).toList();
            default -> throw new IllegalStateException("Unexpected value: " + filterBy);
        };

        List<List<Provider>> sortedListForGettingUserChoice = new ArrayList<>();
        for (int i = 0; i < testSorted.size(); i++) {
            for (int j = 0; j < test.size(); j++) {
                if (test.get(j).equals(testSorted.get(i))) {
                    sortedListForGettingUserChoice.add(i, listForGettingUserChoice.get(j));
                }
            }
        }
        RequestHandler.setSortedListForGettingUserChoice(sortedListForGettingUserChoice);

        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < testSorted.size(); i++) {
            stringBuilder.append("<br><form actions=\"\" method=\"post\">");
            stringBuilder.append(STR."<button name=\"routeName\" type=\"submit\" value=\"\{i+1}\">Reserve route nr \{i+1}</button><br>");
            stringBuilder.append(STR."Route path: \{testSorted.get(i).path}<br>");
            stringBuilder.append(STR."Companies: \{testSorted.get(i).company}<br>");
            stringBuilder.append(STR."Price: \{testSorted.get(i).price} credits | travel time \{testSorted.get(i).travelTime} | distance \{testSorted.get(i).distance} km<br>");
            stringBuilder.append("</form><br>");
        }
        RequestHandler.setPath(stringBuilder.toString());
    }
}



