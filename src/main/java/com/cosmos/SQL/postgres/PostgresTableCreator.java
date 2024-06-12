package com.cosmos.SQL.postgres;

import com.cosmos.SQL.SQLDatabaseTableCreator;
import com.cosmos.SQL.postgres.initiator.Planet;
import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.*;
import java.util.ArrayList;
import java.util.UUID;

public class PostgresTableCreator implements SQLDatabaseTableCreator {

    private JSONObject apiData;
    private final Connection connection;

    public PostgresTableCreator (Connection connection) {
        this.connection = connection;

    }

    public void createAllTables(JSONObject apiData) throws SQLException {
        this.apiData = apiData;
        insertPriceListTable();
        insertPlanetTable();
        insertRouteInfoTable();
        insertCompanyTable();
        insertProviderTable();
    }

    private void insertPriceListTable() throws SQLException {
        String uuid = apiData.getString("id");
        String validUntil = apiData.getString("validUntil");
        String validUntilFormatted = validUntil.replace("T", " ").replace("Z", "");
        Timestamp timestamp = Timestamp.valueOf(validUntilFormatted);

        String sql = "INSERT INTO price_list(" +
                "uuid, valid_until)" +
                "VALUES (?, ?)";

        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        preparedStatement.setObject(1, UUID.fromString(uuid));
        preparedStatement.setTimestamp(2, timestamp);
        preparedStatement.execute();
    }

    private void insertPlanetTable() throws SQLException {
        String sql = "INSERT INTO planet(" +
                "uuid, name)" +
                "VALUES (?, ?)";
        PreparedStatement preparedStatement = connection.prepareStatement(sql);

        JSONArray legsArray = apiData.getJSONArray("legs");
        for (int i = 0; i < legsArray.length(); i++) {
            JSONObject legsObject = (JSONObject) legsArray.get(i);
            JSONObject routeInfo = (JSONObject) legsObject.get("routeInfo");
            JSONObject from = (JSONObject) routeInfo.get("from");
            JSONObject to = (JSONObject) routeInfo.get("to");

            String sqlRead = "SELECT name FROM planet;";
            PreparedStatement readStatement = connection.prepareStatement(sqlRead);
            ResultSet resultSet = readStatement.executeQuery();

            ArrayList<String> planetList = new ArrayList<>();
            while (resultSet.next()) {
                planetList.add(resultSet.getString(1));
            }

            String name = from.getString("name");
            checkUniquesAndInsert(planetList, name, preparedStatement);
            name = to.getString("name");
            checkUniquesAndInsert(planetList, name, preparedStatement);
        }
    }

    private void checkUniquesAndInsert(ArrayList<String> planetList, String name, PreparedStatement preparedStatement) throws SQLException {
        boolean isNameUnique = true;
        for (String s : planetList) {
            if (name.equals(s)) {
                isNameUnique = false;
                break;
            }
        }
        if (isNameUnique) {
            UUID uuid = UUID.randomUUID();
            preparedStatement.setObject(1, uuid);
            preparedStatement.setString(2, name);
            preparedStatement.execute();
        }
    }
    private void insertRouteInfoTable() throws SQLException {
        String sql = "INSERT INTO route_info(" +
                "uuid, price_list_uuid, from_planet_uuid, to_planet_uuid, distance)" +
                "VALUES (?, ?, ?, ?, ?)";
        PreparedStatement preparedStatement = connection.prepareStatement(sql);

        String sqlReadPriceList = "SELECT uuid FROM price_list;";
        PreparedStatement readStatement = connection.prepareStatement(sqlReadPriceList);
        ResultSet resultSet = readStatement.executeQuery();
        resultSet.next();
        String priceListUuid = resultSet.getString(1);

        JSONArray legsArray = apiData.getJSONArray("legs");
        for (int i = 0; i < legsArray.length(); i++) {
            JSONObject legsObject = (JSONObject) legsArray.get(i);
            JSONObject routeInfo = (JSONObject) legsObject.get("routeInfo");
            String id = routeInfo.getString("id");
            long distance = routeInfo.getLong("distance");
            JSONObject from = (JSONObject) routeInfo.get("from");
            JSONObject to = (JSONObject) routeInfo.get("to");

            String sqlReadPlanet = "SELECT * FROM planet;";
            readStatement = connection.prepareStatement(sqlReadPlanet);
            resultSet = readStatement.executeQuery();

            ArrayList<Planet> planetList = new ArrayList<>();
            while (resultSet.next()) {
                planetList.add(new Planet(resultSet.getString(1), resultSet.getString(2)));
            }

            preparedStatement.setObject(1,UUID.fromString(id));
            preparedStatement.setObject(2,UUID.fromString(priceListUuid));
            String name = from.getString("name");
            for (Planet planet : planetList) {
                if (name.equals(planet.getName())) {
                    preparedStatement.setObject(3, UUID.fromString(planet.getUuid()));
                    break;
                }
            }
            name = to.getString("name");
            for (Planet planet : planetList) {
                if (name.equals(planet.getName())) {
                    preparedStatement.setObject(4, UUID.fromString(planet.getUuid()));
                    break;
                }
            }
            preparedStatement.setLong(5, distance);
            preparedStatement.execute();
        }
    }

    private void insertCompanyTable() throws SQLException {
        String sql = "INSERT INTO company(" +
                "uuid, name)" +
                "VALUES (?, ?)";
        PreparedStatement preparedStatement = connection.prepareStatement(sql);

        JSONArray legsArray = apiData.getJSONArray("legs");
        for (int i = 0; i < legsArray.length(); i++) {
            JSONObject legsObject = (JSONObject) legsArray.get(i);
            JSONArray providersArray = (JSONArray) legsObject.get("providers");
            for (int j = 0; j < providersArray.length(); j++) {
                JSONObject providersObject = (JSONObject) providersArray.get(j);
                JSONObject company = (JSONObject) providersObject.get("company");
                String id = company.getString("id");
                String name = company.getString("name");

                String sqlRead = "SELECT uuid FROM company;";
                PreparedStatement readStatement = connection.prepareStatement(sqlRead);
                ResultSet resultSet = readStatement.executeQuery();

                ArrayList<String> planetList = new ArrayList<>();
                while (resultSet.next()) {
                    planetList.add(resultSet.getString(1));
                }
                boolean isNameUnique = true;
                for (String s : planetList) {
                    if (id.equals(s)) {
                        isNameUnique = false;
                        break;
                    }
                }
                if (isNameUnique) {
                    preparedStatement.setObject(1, UUID.fromString(id));
                    preparedStatement.setString(2, name);
                    preparedStatement.execute();
                }
            }
        }
    }

    private void insertProviderTable() throws SQLException {
        String sql = "INSERT INTO provider(" +
                "uuid, company_uuid, route_info_uuid, price, flight_start, flight_end)" +
                "VALUES (?, ?, ?, ?, ?, ?)";
        PreparedStatement preparedStatement = connection.prepareStatement(sql);

        JSONArray legsArray = apiData.getJSONArray("legs");
        for (int i = 0; i < legsArray.length(); i++) {
            JSONObject legsObject = (JSONObject) legsArray.get(i);
            JSONObject routeInfo = (JSONObject) legsObject.get("routeInfo");
            String routeInfoId = routeInfo.getString("id");
            JSONArray providersArray = (JSONArray) legsObject.get("providers");
            for (int j = 0; j < providersArray.length(); j++) {
                JSONObject providersObject= (JSONObject) providersArray.get(j);
                String providersId = providersObject.getString("id");
                JSONObject company = (JSONObject) providersObject.get("company");
                String companyId = company.getString("id");
                double price = providersObject.getDouble("price");
                String flightStart = providersObject.getString("flightStart");
                String flightStartFormatted = flightStart.replace("T", " ").replace("Z", "");
                String flightEnd = providersObject.getString("flightEnd");
                String flightEndFormatted = flightEnd.replace("T", " ").replace("Z", "");

                preparedStatement.setObject(1, UUID.fromString(providersId));
                preparedStatement.setObject(2, UUID.fromString(companyId));
                preparedStatement.setObject(3, UUID.fromString(routeInfoId));
                preparedStatement.setDouble(4, price);
                preparedStatement.setTimestamp(5, Timestamp.valueOf(flightStartFormatted));
                preparedStatement.setTimestamp(6, Timestamp.valueOf(flightEndFormatted));
                preparedStatement.execute();
            }
        }
    }
}
