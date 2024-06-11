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

    List<List<String>> travelList;
    static List<Route> travelMap = new ArrayList<>();
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
           travelMap.add(new Route(resultSet.getString(3), resultSet.getString(4)));
        }
    }

    private void findRoutes(String originPlanet, String destinationPlanet, List<String> currentPath, List<String> allPaths, List<String> currentPathPrettyPrint) {
        currentPath.add(originPlanet);

        for (int i = 0; i < planetList.size(); i++) {
            if (planetList.get(i).uuid.equals(originPlanet)) {
                currentPathPrettyPrint.add(planetList.get(i).name);
                break;
            }
        }

        if (originPlanet.equals(destinationPlanet)) {
            travelList.add(currentPath);
            allPaths.add(String.join(" -> ", currentPathPrettyPrint));
        }
        for (int i = 0; i < travelMap.size(); i++) {
            if (travelMap.get(i).originPlanetUuid.equals(originPlanet)) {
                String destination = travelMap.get(i).destinationPlanetUuid;
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
        List<String> allPaths = new ArrayList<>();
        travelList = new ArrayList<>();
        findRoutes(originPlanet, destinationPlanet, currentPath, allPaths, currentPathPrettyPrint);
        for (int i = 0; i < travelList.size(); i++) {
            for (int j = 0; j < travelList.get(i).size(); j++) {
                System.out.println(travelList.get(i).get(j));
            }
            System.out.println();
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
        for (String path : allPaths) {
            System.out.println(path);
        }
    }
}
