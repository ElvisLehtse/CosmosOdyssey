package com.cosmos.SQL.postgres;

import com.cosmos.SQL.postgres.initiator.*;
import com.cosmos.server.RequestHandler;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This class computes the best possible choices per price or travel time and filters
 * companies based on end user choice.
 */
public class BestDealCalculator {

    static class PrettyPrintInformation {
        public Long getPrice() {
            return price;
        }
        public Long getDistance() {
            return distance;
        }
        Long price;
        String travelTime;
        Long distance;
        String path;
        String company;

        public PrettyPrintInformation(long price, String travelTime, Long distance, String path, String company) {
            this.price = price;
            this.travelTime = travelTime;
            this.distance = distance;
            this.path = path;
            this.company = company;
        }
    }

    public BestDealCalculator () {
        ReadValidDataFromDatabase readValidDataFromDatabase = new ReadValidDataFromDatabase();
        routeList = readValidDataFromDatabase.getRouteList();
        planetList = readValidDataFromDatabase.getPlanetList();
        providerList = readValidDataFromDatabase.getProviderList();
        companyList = readValidDataFromDatabase.getCompanyList();
    }
    static List<RouteInfo> routeList = new ArrayList<>();
    static List<Planet> planetList = new ArrayList<>();
    static List<Provider> providerList = new ArrayList<>();
    static List<Company> companyList = new ArrayList<>();

    /**
     * Takes in the names of the planets selected by the end user and
     * returns their respective UUID-s.
     */
    private List<String> planetSetup(String originPlanet, String destinationPlanet) {
        List<String> userDefinedPlanetNames = new ArrayList<>();
        userDefinedPlanetNames.add(originPlanet);
        userDefinedPlanetNames.add(destinationPlanet);
        List<String> listOfDefinedPlanetUuid = new ArrayList<>();
        for (String selectedPlanets : userDefinedPlanetNames) {
            for (Planet planet : planetList) {
                if (selectedPlanets.equals(planet.name())) {
                    listOfDefinedPlanetUuid.add(planet.uuid());
                }
            }
        }
        return listOfDefinedPlanetUuid;
    }

    /**
     * Takes in the names of the companies selected by the end user and
     * returns their respective UUID-s.
     */
    private List<String> companySetup(List<String> userDefinedCompanyNames) {
        List<String> listOfDefinedCompanyUuid = new ArrayList<>();
        for (String selectedCompanies : userDefinedCompanyNames) {
            for (Company company : companyList) {
                if (selectedCompanies.equals(company.name())) {
                    listOfDefinedCompanyUuid.add(company.uuid());
                }
            }
        }
        return listOfDefinedCompanyUuid;
    }

    /**
     * This method initiates other methods responsible for generating suitable route and provider information,
     * providing initial settings for them.
     */
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

    /**
     * This method is called when no companies are filtered by the end user. It provides
     * all valid companies from the database to select from.
     */
    public void generateUnfilteredSolutions(String originPlanet, String destinationPlanet, String filterBy) {
        List<String> allCompaniesList = new ArrayList<>();
        for (Company company : companyList) {
            allCompaniesList.add(company.name());
        }
        generateSolutions(originPlanet, destinationPlanet, allCompaniesList, filterBy);
    }

    /**
     * Finds all possible routes from origin planet to destination planet.
     * @return routes as lists within a list, containing routeInfo.
     * The outer list contains different route patterns such as 1: Jupiter -> Venus and 2: Jupiter -> Mars -> Venus
     * The inner list contains routeInfo to a specific route pattern,
     * ie in case 2, it has the routeInfo for A: Jupiter -> Mars and B: Mars -> Venus
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
     * Filters all possible route patterns by companies provided by the end user.
     * Filtered routes are sorted by price or travel time. A maximum number of providers is also added.
     * @return routes with provider information as a list within a list,
     * containing provider uuid, company uuid, route info uuid, price, flight start and flight end information.
     */
    private List<List<Provider>> suitableProvidersFiltered(List<String> companyListProvidedByUser, List<List<RouteInfo>> allPossibleRoutes, String filterBy) {
        int maximumLimitOfProvidersPerRoute = 3;

        //This filters out any companies not chosen by the end user
        List<List<Provider>> suitableProvidersByRoute = new ArrayList<>();
        List<Provider> suitableProvidersByCompany = new ArrayList<>();
        for (Provider provider : providerList) {
            if (companyListProvidedByUser.contains(provider.company_uuid())) {
                suitableProvidersByCompany.add(provider);
            }
        }

        //Possible providers are grouped by routes
        Map<String, List<Provider>> groupedProviders = suitableProvidersByCompany.stream()
                .collect(Collectors.groupingBy(Provider::route_info_uuid));

        List<Provider> suitableProviders = new ArrayList<>();

        //Suitable providers are sorted based on price of travel time
        //A hard cap of 3 is provided per single route
        if (filterBy.equals("price")) {
            for (Map.Entry<String, List<Provider>> entry : groupedProviders.entrySet()) {
                List<Provider> providers = entry.getValue();
                providers.sort(Comparator.comparingLong(Provider::price));
                suitableProviders.addAll(providers.stream().limit(maximumLimitOfProvidersPerRoute).toList());
            }
        } else if (filterBy.equals("time")) {
            for (Map.Entry<String, List<Provider>> entry : groupedProviders.entrySet()) {
                List<Provider> providers = entry.getValue();
                providers.sort(Comparator.comparingLong(Provider::getTravelTime));
                suitableProviders.addAll(providers.stream().limit(maximumLimitOfProvidersPerRoute).toList());
            }
        }

        //Providers are stored by their respective route patterns
        for (List<RouteInfo> allPossibleRoute : allPossibleRoutes) {
            List<Provider> tempListForSuitableProvidersByRoute = new ArrayList<>();
            for (RouteInfo routeInfo : allPossibleRoute) {
                for (Provider suitableProvider : suitableProviders) {
                    if (routeInfo.getUuid().equals(suitableProvider.route_info_uuid())) {
                        tempListForSuitableProvidersByRoute.add(suitableProvider);
                    }
                }
            }
            suitableProvidersByRoute.add(tempListForSuitableProvidersByRoute);
        }
        return suitableProvidersByRoute;
    }

    /**
     * Finds all possible combinations for each route pattern. Since the possible amount of combinations for
     * a longer route can be incomputably large, each route has a maximum number of providers allowed.
     */
    private Map<Integer, List<List<Provider>>> findAllPossibleProviderCombinations(List<List<RouteInfo>> allPossibleRoutes, List<List<Provider>> suitableProvidersByRoute) {

        //Each route pattern is represented as an integer of the map
        //Each individual route is represented as a member the outer list of the map
        //Routes are assigned a number of possible providers stored within the inner list of the map
        Map<Integer, List<List<Provider>>> allPossibleProvidersTable = new HashMap<>();
        for (int i = 0; i < allPossibleRoutes.size(); i++) {
            List<List<Provider>> tempProviderListOfLists = getListOfPossibleProvidersPerRoutes(allPossibleRoutes, suitableProvidersByRoute, i);
            allPossibleProvidersTable.put(i, tempProviderListOfLists);
        }
        //All possible combinations are combined via recursive method
        //Each route pattern is represented as an integer of the map
        //Possible combinations of providers are represented within the outer list of the map
        //Individual providers are represented within the inner list of the map
        Map<Integer, List<List<Provider>>> allPossibleProviderCombinationForAllRoutes = new HashMap<>();
        for (int i = 0; i < allPossibleProvidersTable.size(); i++) {
            List<List<Provider>> allPossibleProviderCombinations = generateCombinations(allPossibleProvidersTable.get(i));
            allPossibleProviderCombinationForAllRoutes.put(i, allPossibleProviderCombinations);
        }
        return allPossibleProviderCombinationForAllRoutes;
    }

    private static List<List<Provider>> getListOfPossibleProvidersPerRoutes(List<List<RouteInfo>> allPossibleRoutes, List<List<Provider>> suitableProvidersByRoute, int i) {
        List<List<Provider>> tempProviderListOfLists = new ArrayList<>();
        for (int j = 0; j < allPossibleRoutes.get(i).size(); j++) {
            List<Provider> tempProviderList = new ArrayList<>();
            for (int k = 0; k < suitableProvidersByRoute.get(i).size(); k++) {
                if (suitableProvidersByRoute.get(i).get(k).route_info_uuid().equals(allPossibleRoutes.get(i).get(j).getUuid())) {
                    tempProviderList.add(suitableProvidersByRoute.get(i).get(k));
                }
            }
            tempProviderListOfLists.add(tempProviderList);
        }
        return tempProviderListOfLists;
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
            current.removeLast();
        }
    }

    /**
     * Calculates the total distances for all possible routes.
     * Route distances are returned as a list.
     */
    private static List<Long> routesDistance(List<List<RouteInfo>> allPossibleRoutes) {
        List<Long> routeDistance = new ArrayList<>();
        for (List<RouteInfo> allPossibleRoute : allPossibleRoutes) {
            long singleRouteDistance = 0L;
            for (RouteInfo routeInfo : allPossibleRoute) {
                singleRouteDistance = singleRouteDistance + routeInfo.getDistance();
            }
            routeDistance.add(singleRouteDistance);
        }
        return routeDistance;
    }

    /**
     * Calculates the total price of all the routes and returns them as a map.
     * The integer represents the route pattern.
     * The list contains prices of corresponding combinations of providers specific to a route pattern.
     */
    private Map<Integer, List<Long>> totalPriceTagForAllProviders(Map<Integer, List<List<Provider>>> allPossibleProviderCombinationForAllRoutes) {
        Map<Integer, List<Long>> totalPriceMap = new HashMap<>();
        for (int i = 0; i < allPossibleProviderCombinationForAllRoutes.size(); i++) {
            List<Long> totalPrice = new ArrayList<>();
            for (int j = 0; j < allPossibleProviderCombinationForAllRoutes.get(i).size(); j++) {
                List<Provider> suitableProviders = allPossibleProviderCombinationForAllRoutes.get(i).get(j);
                long singleRoutePrice = 0;
                for (Provider providers : suitableProviders) {
                    singleRoutePrice = singleRoutePrice + providers.price();
                }
                totalPrice.add(singleRoutePrice);
            }
            totalPriceMap.put(i, totalPrice);
        }
        return totalPriceMap;
    }

    /**
     * Calculates the total time for routes.
     * The integer represents the route pattern.
     * The list contains the total time of corresponding combinations of providers specific to a route pattern.
     */
    private Map<Integer, List<Long>> totalTravelTimeForAllRoutePatterns(Map<Integer, List<List<Provider>>> allPossibleProviderCombinationForAllRoutes) {
        Map<Integer, List<Long>> travelTimeForAll = new HashMap<>();
        for (int i = 0; i < allPossibleProviderCombinationForAllRoutes.size(); i++) {
            travelTimeForAll.put(i, totalTravelTimeForRoutes(allPossibleProviderCombinationForAllRoutes.get(i)));
        }
        return travelTimeForAll;
    }

    private List<Long> totalTravelTimeForRoutes(List<List<Provider>> suitableProviders) {
        List<Long> travelTime = new ArrayList<>();
        for (List<Provider> suitableProvider : suitableProviders) {
            long tempHours;
            long hours = 0;
            for (Provider provider : suitableProvider) {
                tempHours = Duration.between(provider.flight_start().toLocalDateTime(), provider.flight_end().toLocalDateTime()).toHours();
                hours = hours + tempHours;
            }
            travelTime.add(hours);
        }
        return travelTime;
    }

    /**
     * Creates a list of lists where the outer list refers to the route pattern
     * and the inner list to the names of the companies used per provider combination.
     * The companies are linked together to form a travel path.
     */
    private List<List<String>> companiesUsedForTravel(Map<Integer, List<List<Provider>>> allPossibleProviderCombinationForAllRoutes) {
        List<List<String>> companies = new ArrayList<>();
        for (int i = 0; i < allPossibleProviderCombinationForAllRoutes.size(); i++) {
            List<List<Provider>> suitableProviders = allPossibleProviderCombinationForAllRoutes.get(i);
            List <String> selectedCompanies = new ArrayList<>();
            for (List<Provider> suitableProvider : suitableProviders) {
                List<String> companyNames = new ArrayList<>();
                for (Provider provider : suitableProvider) {
                    for (Company company : companyList) {
                        if (provider.company_uuid().equals(company.uuid())) {
                            companyNames.add(company.name());
                        }
                    }
                }
                selectedCompanies.add(String.join(" -> ", companyNames));
            }
            companies.add(selectedCompanies);
        }
        return companies;
    }

    private String travelTimeToString(Long totalTravelTime) {
        int hoursInDay = 24;
        long hours = totalTravelTime;
        long days = hours / hoursInDay;
        hours = hours - (days * hoursInDay);
        return STR."\{days}d \{hours}h";
    }

    /**
     * Creates routes filtered by companies, providing the best prices and travel distances
     * along with company names being used.
     */
    private void prettyPrintPaths(Map<Integer, List<List<Provider>>> allPossibleProviderCombinationForAllRoutes, List<List<RouteInfo>> allPossibleRoutes, String originPlanet, String filterBy) {
        int showMaxNumberOfCombinationsPerRoute = 3;
        List<Long> totalDistance = routesDistance(allPossibleRoutes);
        Map<Integer, List<Long>> totalTravelTime = totalTravelTimeForAllRoutePatterns(allPossibleProviderCombinationForAllRoutes);
        Map<Integer, List<Long>> totalPrice = totalPriceTagForAllProviders(allPossibleProviderCombinationForAllRoutes);
        List<List<String>> companies = companiesUsedForTravel(allPossibleProviderCombinationForAllRoutes);
        List<PrettyPrintInformation> prettyPrintInformation = new ArrayList<>();
        List<List<Provider>> listForGettingUserChoice = new ArrayList<>();
        for (int i = 0; i < allPossibleProviderCombinationForAllRoutes.size(); i++) {
            for (int j = 0; j < allPossibleProviderCombinationForAllRoutes.get(i).size() && j < showMaxNumberOfCombinationsPerRoute; j++) {
                List<RouteInfo> allPath = allPossibleRoutes.get(i);
                List<String> planetName = new ArrayList<>();
                planetName.add(originPlanet);
                for (RouteInfo path : allPath) {
                    for (Planet planet : planetList) {
                        if (planet.uuid().equals(path.getDestinationPlanetUuid())) {
                            planetName.add(planet.name());
                            break;
                        }
                    }
                }
                prettyPrintInformation.add(new PrettyPrintInformation(totalPrice.get(i).get(j), travelTimeToString(totalTravelTime.get(i).get(j)), totalDistance.get(i), String.join(" -> ", planetName), companies.get(i).get(j)));
                listForGettingUserChoice.add(allPossibleProviderCombinationForAllRoutes.get(i).get(j));
            }
        }
        List<PrettyPrintInformation> prettyPrintInformationSorted = switch (filterBy) {
            case "price" -> prettyPrintInformation.stream().sorted(Comparator.comparing(PrettyPrintInformation::getDistance).thenComparing(PrettyPrintInformation::getPrice)).toList();
            case "time" -> prettyPrintInformation.stream().sorted(Comparator.comparing(PrettyPrintInformation::getDistance)).toList();
            default -> throw new IllegalStateException(STR."Unexpected value: \{filterBy}");
        };

        List<List<Provider>> sortedListForGettingUserChoice = new ArrayList<>();
        for (int i = 0; i < prettyPrintInformationSorted.size(); i++) {
            for (int j = 0; j < prettyPrintInformation.size(); j++) {
                if (prettyPrintInformation.get(j).equals(prettyPrintInformationSorted.get(i))) {
                    sortedListForGettingUserChoice.add(i, listForGettingUserChoice.get(j));
                }
            }
        }
        RequestHandler.setSortedListForGettingUserChoice(sortedListForGettingUserChoice);

        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < prettyPrintInformationSorted.size(); i++) {
            stringBuilder.append("<br><form actions=\"\" method=\"post\">");
            stringBuilder.append(STR."<button name=\"routeName\" type=\"submit\" value=\"\{i+1}\">Reserve route nr \{i+1}</button><br>");
            stringBuilder.append(STR."Route path: \{prettyPrintInformationSorted.get(i).path}<br>");
            stringBuilder.append(STR."Companies: \{prettyPrintInformationSorted.get(i).company}<br>");
            stringBuilder.append(STR."Price: \{prettyPrintInformationSorted.get(i).price} credits | travel time \{prettyPrintInformationSorted.get(i).travelTime} | distance \{prettyPrintInformationSorted.get(i).distance} km<br>");
            stringBuilder.append("</form><br>");
        }
        RequestHandler.setPath(stringBuilder.toString());
    }
}