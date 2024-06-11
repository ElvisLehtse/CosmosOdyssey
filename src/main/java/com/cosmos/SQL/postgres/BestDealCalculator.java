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
    static List<Route> travelList = new ArrayList<>();
    static List<Planet> planetList = new ArrayList<>();

    static class Route {
        String originPlanetUuid;
        String destinationPlanetUuid;

        public Route (String originPlanetUuid, String destinationPlanetUuid) {
            this.originPlanetUuid = originPlanetUuid;
            this.destinationPlanetUuid = destinationPlanetUuid;
        }
    }

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
           travelList.add(new Route(resultSet.getString(3), resultSet.getString(4)));
        }
    }

    private void findRoutes(String originPlanet, String destinationPlanet, List<String> currentPath, List<List<String>> allPaths, List<String> currentPathPrettyPrint) {
        currentPath.add(originPlanet);

        if (originPlanet.equals(destinationPlanet)) {
            allPaths.add(currentPath);
        }
        for (int i = 0; i < travelList.size(); i++) {
            if (travelList.get(i).originPlanetUuid.equals(originPlanet)) {
                String destination = travelList.get(i).destinationPlanetUuid;
                if (!currentPath.contains(destination)) {
                    findRoutes(destination, destinationPlanet, new ArrayList<>(currentPath), allPaths, new ArrayList<>(currentPathPrettyPrint));
                }
            }
        }
    }

    public void calculateBestPrice(String originPlanet, String destinationPlanet) throws SQLException {
        initiatePlanets();
        initiateRoutes();
        List<String> currentPath = new ArrayList<>();
        List<String> currentPathPrettyPrint = new ArrayList<>();
        List<List<String>> allPaths = new ArrayList<>();
        findRoutes(originPlanet, destinationPlanet, currentPath, allPaths, currentPathPrettyPrint);
        prettyPrint(allPaths, originPlanet, destinationPlanet);
    }

    private void prettyPrint(List<List<String>> allPaths, String originPlanet, String destinationPlanet) {
        List<String> prettyPrintPaths = new ArrayList<>();
        for (int i = 0; i < allPaths.size(); i++) {
            List<String> tempList = new ArrayList<>();
            for (int j = 0; j < allPaths.get(i).size(); j++) {
                for (int k = 0; k < planetList.size(); k++) {
                    if (planetList.get(k).uuid.equals(allPaths.get(i).get(j))) {
                        tempList.add(planetList.get(k).name);
                        break;
                    }
                }
            }
            prettyPrintPaths.add(String.join(" -> ", tempList));
        }

        String origin = "";
        String destination = "";
        for (int i = 0; i < planetList.size(); i++) {
            if (planetList.get(i).uuid.equals(originPlanet)) {
                origin = planetList.get(i).name;
                break;
            }
        }
        for (int i = 0; i < planetList.size(); i++) {
            if (planetList.get(i).uuid.equals(destinationPlanet)) {
                destination = planetList.get(i).name;
                break;
            }
        }

        System.out.println("All possible routes from " + origin + " to " + destination + ":");
        for (String path : prettyPrintPaths) {
            System.out.println(path);
        }
    }
}

