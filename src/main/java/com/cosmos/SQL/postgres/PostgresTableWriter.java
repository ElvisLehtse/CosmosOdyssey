package com.cosmos.SQL.postgres;

import com.cosmos.SQL.SQLDatabaseTableWriter;
import com.cosmos.SQL.postgres.initiator.Planet;
import com.cosmos.SQL.postgres.initiator.Provider;
import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * This class inserts the data provided by the API to the database.
 */
public class PostgresTableWriter implements SQLDatabaseTableWriter {

    private JSONObject apiData;
    private static Connection connection;

    public PostgresTableWriter(Connection connection) {
        PostgresTableWriter.connection = connection;
    }

    /**
     * This method is called when the database is created for the first time.
     * Calls the methods to insert apiData to the database's respective tables.
     * @return a valid price list back to the caller.
     */
    public String insertDataToAllTables(JSONObject apiData) throws SQLException {
        this.apiData = apiData;
        String priceListUuid = insertPriceListTable();
        insertPlanetTable();
        insertRouteInfoTable(priceListUuid);
        insertCompanyTable();
        insertProviderTable();
        return priceListUuid;
    }

    /**
     * This method is called when the database requires an update.
     * Calls the methods to insert apiData to the database's respective tables.
     * @return a valid price list back to the caller.
     */
    public String insertDataToTables(JSONObject apiData) throws SQLException {
        this.apiData = apiData;
        String priceListUuid = insertPriceListTable();
        insertRouteInfoTable(priceListUuid);
        insertCompanyTable();
        insertProviderTable();
        return priceListUuid;
    }

    /**
     * Inserts the price list information to the database.
     * @return the price list UUID provided by the API.
     */
    private String insertPriceListTable() throws SQLException {
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
        return uuid;
    }

    /**
     * Inserts planet information to the database.
     */
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

    /**
     * Checks for the uniqueness of the names of the planets. If the planet does not exist, it is added
     * to the database. If it exists, duplicates are ignored. A new unique UUID is added for each planet.
     */
    private void checkUniquesAndInsert(ArrayList<String> planetList, String name, PreparedStatement preparedStatement) throws SQLException {
        boolean isNameUnique = !planetList.contains(name);
        if (isNameUnique) {
            UUID uuid = UUID.randomUUID();
            preparedStatement.setObject(1, uuid);
            preparedStatement.setString(2, name);
            preparedStatement.execute();
        }
    }

    /**
     * Inserts route information to the database.
     */
    private void insertRouteInfoTable(String priceListUuid) throws SQLException {
        String sql = "INSERT INTO route_info(" +
                "uuid, price_list_uuid, from_planet_uuid, to_planet_uuid, distance)" +
                "VALUES (?, ?, ?, ?, ?)";
        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        JSONArray legsArray = apiData.getJSONArray("legs");
        for (int i = 0; i < legsArray.length(); i++) {
            JSONObject legsObject = (JSONObject) legsArray.get(i);
            JSONObject routeInfo = (JSONObject) legsObject.get("routeInfo");
            String id = routeInfo.getString("id");
            long distance = routeInfo.getLong("distance");
            JSONObject from = (JSONObject) routeInfo.get("from");
            JSONObject to = (JSONObject) routeInfo.get("to");

            String sqlReadPlanet = "SELECT * FROM planet;";
            PreparedStatement readStatement = connection.prepareStatement(sqlReadPlanet);
            ResultSet resultSet = readStatement.executeQuery();

            ArrayList<Planet> planetList = new ArrayList<>();
            while (resultSet.next()) {
                planetList.add(new Planet(resultSet.getString(1), resultSet.getString(2)));
            }
            preparedStatement.setObject(1,UUID.fromString(id));
            preparedStatement.setObject(2,UUID.fromString(priceListUuid));
            String name = from.getString("name");

            for (Planet planet : planetList) {
                if (name.equals(planet.name())) {
                    preparedStatement.setObject(3, UUID.fromString(planet.uuid()));
                    break;
                }
            }
            name = to.getString("name");
            for (Planet planet : planetList) {
                if (name.equals(planet.name())) {
                    preparedStatement.setObject(4, UUID.fromString(planet.uuid()));
                    break;
                }
            }
            preparedStatement.setLong(5, distance);
            preparedStatement.execute();
        }
    }

    /**
     * Inserts company information to the database.
     */
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

    /**
     * Inserts provider information to the database.
     */
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

    /**
     * Inserts reservation information to the database.
     */
    public void storeUserChoice(List<List<Provider>> sortedListForGettingUserChoice, int routeNumber, String userFirstName, String userLastName) {
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
        for (int i = 0; i < sortedListForGettingUserChoice.get(routeNumber - 1).size(); i++) {
            try {
                UUID reservedRoutesUuid = UUID.randomUUID();
                PreparedStatement preparedStatement = connection.prepareStatement(reservedRoutes);
                preparedStatement.setObject(1, reservedRoutesUuid);
                preparedStatement.setObject(2, reservationUuid);
                preparedStatement.setObject(3, UUID.fromString(sortedListForGettingUserChoice.get(routeNumber - 1).get(i).uuid()));
                preparedStatement.execute();
            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }
        }
    }
}
