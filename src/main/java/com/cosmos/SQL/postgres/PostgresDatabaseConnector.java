package com.cosmos.SQL.postgres;

import com.cosmos.APIReader;
import com.cosmos.SQL.SQLDatabaseTableCreator;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.*;
import java.util.Scanner;

public class PostgresDatabaseConnector {

    private static String host;
    private static String port;
    private static String database;
    private static String username;
    private static String password;

    public static Connection connection() throws SQLException {
        return DriverManager.getConnection("jdbc:postgresql://" + host + ":" + port + "/" + database, username, password);
    }
    private static void settings() {
        File file = new File("Postgres credentials.txt");
        try {
            Scanner scanner = new Scanner(file);
            host = "localhost";
            port = "5432";
            database = "cosmosodyssey";
            username = scanner.nextLine().replace("user=", "");
            password = scanner.nextLine().replace("pass=", "");
        } catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
        }
    }

    private static String getDatabaseResponse(String sql, Connection connection) throws SQLException {
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery(sql);
        String returnString = "";
        if (resultSet.next()) {
            returnString = resultSet.getString(1);
        }
        return returnString;
    }

    public static String checkIfDatabaseExists() throws IOException {
        String sql = "SELECT EXISTS(SELECT datname FROM pg_catalog.pg_database WHERE datname = 'cosmosodyssey');";
        String priceListUuid = "";
        settings();
        try (Connection connection = DriverManager.getConnection("jdbc:postgresql://" + host + ":" + port + "/", username, password)) {
            String databaseStatus = getDatabaseResponse(sql, connection);
            if (databaseStatus.equals("f")) {
                priceListUuid = createNewDatabase();
            } else if (databaseStatus.equals("t")) {
                priceListUuid = checkIfDatabaseHasValidPriceList();
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return priceListUuid;
    }

    private static String checkIfDatabaseHasValidPriceList() throws IOException {
        String sql = "SELECT uuid FROM price_list WHERE valid_until > NOW() - INTERVAL '3h';";
        String priceListUuid = "";
        try (Connection connection = connection()) {
            priceListUuid = getDatabaseResponse(sql, connection);
            if (priceListUuid.isEmpty()) {
                priceListUuid = addNewPriceList();
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return priceListUuid;
    }

    private static String addNewPriceList() throws IOException {
        APIReader apiReader = new APIReader();
        JSONObject apiData = apiReader.getJsonDataFromAPI();
        String priceListUuid = "";
        try (Connection connection = connection()) {
            SQLDatabaseTableCreator sqlDatabaseTableCreator = new PostgresTableCreator(connection);
            priceListUuid = sqlDatabaseTableCreator.createTables(apiData);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return priceListUuid;
    }

    private static String createNewDatabase() {
        String createNewDatabase = "CREATE DATABASE CosmosOdyssey";
        String priceListUuid = "";
        try (Connection connection = DriverManager.getConnection("jdbc:postgresql://" + host + ":" + port + "/", username, password)) {
            Statement statement = connection.createStatement();
            statement.execute(createNewDatabase);
            try (Connection newConnection = connection()) {
                File file = new File("Create SQL tables.txt");
                Scanner scanner = new Scanner(file);
                StringBuilder stringBuilder = new StringBuilder();
                while (scanner.hasNextLine()) {
                    stringBuilder.append(scanner.nextLine());
                }
                String createTablesSQLFullText = stringBuilder.toString();
                statement = newConnection.createStatement();
                statement.execute(createTablesSQLFullText);
                APIReader apiReader = new APIReader();
                JSONObject apiData = apiReader.getJsonDataFromAPI();
                SQLDatabaseTableCreator sqlDatabaseTableCreator = new PostgresTableCreator(newConnection);
                priceListUuid = sqlDatabaseTableCreator.createAllTables(apiData);
            } catch (SQLException | IOException e) {
                System.out.println(e.getMessage());
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return priceListUuid;
    }
}
