package com.cosmos.SQL.postgres;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class BestDealCalculator {

    private final Connection connection;
    public BestDealCalculator (Connection connection) {
        this.connection = connection;
    }
    static List<RouteInfo> routeList = new ArrayList<>();
    static List<Planet> planetList = new ArrayList<>();
    static List<Provider> providerList = new ArrayList<>();
    static List<Company> companyList = new ArrayList<>();


    private void initiatePlanets() throws SQLException {
        String sql= "SELECT * FROM planet;";
        PreparedStatement readStatement = connection.prepareStatement(sql);
        ResultSet resultSet = readStatement.executeQuery();

        while (resultSet.next()) {
            planetList.add(new Planet(resultSet.getString(1), resultSet.getString(2)));
        }
    }

    private void initiateRoutes() throws SQLException {
        String sql= "SELECT * FROM route_info;";
        PreparedStatement readStatement = connection.prepareStatement(sql);
        ResultSet resultSet = readStatement.executeQuery();

        while (resultSet.next()) {
           routeList.add(new RouteInfo(resultSet.getString(1), resultSet.getString(2),
                   resultSet.getString(3), resultSet.getString(4), resultSet.getLong(5)));
        }
    }

    private void initiateProviders() throws SQLException {
        String sql= "SELECT * FROM provider;";
        PreparedStatement readStatement = connection.prepareStatement(sql);
        ResultSet resultSet = readStatement.executeQuery();

        while (resultSet.next()) {
            providerList.add(new Provider(resultSet.getString(1), resultSet.getString(2), resultSet.getString(3),
                    resultSet.getInt(4), resultSet.getTimestamp(5), resultSet.getTimestamp(6)));
        }
    }

    private void initiateCompany() throws SQLException {
        String sql= "SELECT * FROM company;";
        PreparedStatement readStatement = connection.prepareStatement(sql);
        ResultSet resultSet = readStatement.executeQuery();

        while (resultSet.next()) {
            companyList.add(new Company(resultSet.getString(1), resultSet.getString(2)));
        }
    }

    private void findAllPossibleRoutes(String originPlanet, String destinationPlanet, List<String> currentPath, List<List<String>> allPaths, List<String> currentPathPrettyPrint, List<String> routeUuid, List<List<String>> allRouteUuid, String uuid) {
        currentPath.add(originPlanet);
        if (uuid != null) {
            routeUuid.add(uuid);
        }
        if (originPlanet.equals(destinationPlanet)) {
            allPaths.add(currentPath);
            allRouteUuid.add(routeUuid);
        }
        for (RouteInfo route : routeList) {
            if (route.getOriginPlanetUuid().equals(originPlanet)) {
                String destination = route.getDestinationPlanetUuid();
                uuid = route.getUuid();
                if (!currentPath.contains(destination)) {
                    findAllPossibleRoutes(destination, destinationPlanet, new ArrayList<>(currentPath), allPaths, new ArrayList<>(currentPathPrettyPrint), new ArrayList<>(routeUuid), allRouteUuid, uuid);
                }
            }
        }
    }

    public void generateSolutions(String originPlanet, String destinationPlanet, List<String> companyList) throws SQLException{
        initiatePlanets();
        initiateRoutes();
        initiateProviders();
        initiateCompany();

        List<String> currentPath = new ArrayList<>();
        List<String> currentPathPrettyPrint = new ArrayList<>();
        List<List<String>> allPaths = new ArrayList<>();
        List<String> routeUuid = new ArrayList<>();
        List<List<String>> allRouteUuid = new ArrayList<>();
        String uuid = null;

        findAllPossibleRoutes(originPlanet, destinationPlanet, currentPath, allPaths, currentPathPrettyPrint, routeUuid, allRouteUuid, uuid);

        if (!companyList.isEmpty()) {
            filterByCompany(companyList, allRouteUuid);
        }

        //prettyPrintPaths(allPaths, originPlanet, destinationPlanet);
        /*
        for (List<String> s : allRouteUuid) {
            System.out.println(s);
        }
         */
    }

    private void filterByCompany(List companyList, List<List<String>> allRouteUuid) {
        List<Provider> suitableProviders = new ArrayList<>();
        List<List<String>> suitableRoutesUuid = new ArrayList<>();
        List<List<String>> newAllRoutesUuid = new ArrayList<>();
        for (int i = 0; i < providerList.size(); i++) {
            Provider companyKey = providerList.get(i);
            if (companyList.contains(companyKey.getCompany_uuid())) {
                suitableProviders.add(providerList.get(i));
            }
        }

        for (int j = 0; j < allRouteUuid.size(); j++) {
            List<String> tempList = new ArrayList<>();
            for (int k = 0; k < allRouteUuid.get(j).size(); k++) {
                for (int i = 0; i < suitableProviders.size(); i++) {
                    Provider routeKey = suitableProviders.get(i);
                    if (allRouteUuid.get(j).get(k).equals(routeKey.getRoute_info_uuid())) {
                        if (!tempList.contains(routeKey.getRoute_info_uuid())) {
                            tempList.add(routeKey.getRoute_info_uuid());
                        }
                    }
                }
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

        for (int i = 0; i < newAllRoutesUuid.size(); i++) {
            System.out.println(newAllRoutesUuid.get(i));
        }
    }

    private void prettyPrintPaths(List<List<String>> allPaths, String originPlanet, String destinationPlanet) {
        List<String> prettyPrint = new ArrayList<>();
        for (List<String> allPath : allPaths) {
            List<String> tempList = new ArrayList<>();
            for (String path : allPath) {
                for (Planet planet : planetList) {
                    if (planet.uuid.equals(path)) {
                        tempList.add(planet.name);
                        break;
                    }
                }
            }
            prettyPrint.add(String.join(" -> ", tempList));
        }

        String origin = "";
        String destination = "";
        for (Planet planet : planetList) {
            if (planet.uuid.equals(originPlanet)) {
                origin = planet.name;
                break;
            }
        }
        for (Planet planet : planetList) {
            if (planet.uuid.equals(destinationPlanet)) {
                destination = planet.name;
                break;
            }
        }

        System.out.println(STR."All possible routes from \{origin} to \{destination}:");
        for (String path : prettyPrint) {
            System.out.println(path);
        }
    }
}

