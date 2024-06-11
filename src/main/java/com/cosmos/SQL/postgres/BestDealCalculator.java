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

    HashMap<String, String> Mercury = new HashMap<>();
    HashMap<String, String> Venus = new HashMap<>();
    HashMap<String, String> Earth = new HashMap<>();
    HashMap<String, String> Mars = new HashMap<>();
    HashMap<String, String> Jupiter = new HashMap<>();
    HashMap<String, String> Saturn = new HashMap<>();
    HashMap<String, String> Uranus = new HashMap<>();
    HashMap<String, String> Neptune = new HashMap<>();
    List<List<String>> travelList;

    static class Route {
        String destination;

        public Route(String destination) {
            this.destination = destination;
        }
    }

    static Map<String, List<Route>> travelMap = new HashMap<>();
    private void initializePlanetTags() throws SQLException {
        List<Planet> planetList = new ArrayList<>();
        String sql = "SELECT * FROM planet;";
        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        ResultSet resultSet = preparedStatement.executeQuery();
        while (resultSet.next()) {
            planetList.add(new Planet(resultSet.getString(1), resultSet.getString(2)));
        }
        for (Planet planet : planetList) {
            switch (planet.name) {
                case "Mercury" -> Mercury.put(planet.uuid, planet.name);
                case "Venus" -> Venus.put(planet.uuid, planet.name);
                case "Earth" -> Earth.put(planet.uuid, planet.name);
                case "Mars" -> Mars.put(planet.uuid, planet.name);
                case "Jupiter" -> Jupiter.put(planet.uuid, planet.name);
                case "Saturn" -> Saturn.put(planet.uuid, planet.name);
                case "Uranus" -> Uranus.put(planet.uuid, planet.name);
                case "Neptune" -> Neptune.put(planet.uuid, planet.name);
            }
        }
    }

    private String getValue(HashMap<String, String> planet) {
        return planet.values().toString().replace("[", "").replace("]", "");
    }

    public void routes() throws SQLException {
        String sql= "SELECT * FROM route_info;";
        PreparedStatement readStatement = connection.prepareStatement(sql);
        ResultSet resultSet = readStatement.executeQuery();

        ArrayList<RouteInfo> routeInfo = new ArrayList<>();
        while (resultSet.next()) {
            routeInfo.add(new RouteInfo(resultSet.getString(1), resultSet.getString(2), resultSet.getString(3), resultSet.getString(4), resultSet.getLong(5)));
        }
    }

    private void initializeRoutes() {
        travelMap.put(getValue(Mercury), Arrays.asList(new Route(getValue(Venus))));
        travelMap.put(getValue(Venus), Arrays.asList((new Route(getValue(Mercury))), new Route(getValue(Earth))));
        travelMap.put(getValue(Earth), Arrays.asList((new Route(getValue(Jupiter))), new Route(getValue(Uranus))));
        travelMap.put(getValue(Mars), Arrays.asList(new Route(getValue(Venus))));
        travelMap.put(getValue(Jupiter), Arrays.asList((new Route(getValue(Venus))), new Route(getValue(Mars))));
        travelMap.put(getValue(Saturn), Arrays.asList((new Route(getValue(Earth))), new Route(getValue(Neptune))));
        travelMap.put(getValue(Uranus), Arrays.asList((new Route(getValue(Saturn))), new Route(getValue(Neptune))));
        travelMap.put(getValue(Neptune), Arrays.asList((new Route(getValue(Mercury))), new Route(getValue(Uranus))));
    }

    private void findRoutes(String originPlanet, String destinationPlanet, List<String> currentPath, List<String> allPaths) {
        currentPath.add(originPlanet);

        if (originPlanet.equals(destinationPlanet)) {
            travelList.add(currentPath);
            allPaths.add(String.join(" -> ", currentPath));
        } else if (travelMap.containsKey(originPlanet)) {
            for (Route route : travelMap.get(originPlanet)) {
                if (!currentPath.contains(route.destination)) {
                    findRoutes(route.destination, destinationPlanet, new ArrayList<>(currentPath), allPaths);
                }
            }
        }
    }

    public void calculateBestPrice(String originPlanet, String destinationPlanet) throws SQLException {
        initializePlanetTags();
        initializeRoutes();

        List<String> currentPath = new ArrayList<>();
        List<String> allPaths = new ArrayList<>();
        travelList = new ArrayList<>();
        findRoutes(originPlanet, destinationPlanet, currentPath, allPaths);
        for (int i = 0; i < travelList.size(); i++) {
            for (int j = 0; j < travelList.get(i).size(); j++) {
                System.out.println(travelList.get(i).get(j));
            }
            System.out.println();
        }

        System.out.println("All possible routes from " + originPlanet + " to " + destinationPlanet + ":");
        for (String path : allPaths) {
            System.out.println(path);
        }
    }
}
