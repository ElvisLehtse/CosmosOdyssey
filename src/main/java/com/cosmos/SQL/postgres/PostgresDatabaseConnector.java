package com.cosmos.SQL.postgres;

import com.cosmos.APIReader;
import com.cosmos.SQL.SQLDatabaseTableWriter;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.Scanner;

/**
 * This class connects to the PostgreSQL server, creates the database and its tables if not existing already
 * and calls for the updating of the database with valid price list data.
 */
public class PostgresDatabaseConnector {
    private static String host;
    private static String port;
    private static String database;
    private static String username;
    private static String password;

    public static Connection connection() throws SQLException {
        return DriverManager.getConnection("jdbc:postgresql://" + host + ":" + port + "/" + database, username, password);
    }

    /**
     * Provides information for connecting with PostgreSQL database.
     */
    private static void settings() {
        File file = new File("Postgres credentials.txt");
        try {
            Scanner scanner = new Scanner(file);
            host = scanner.nextLine().replace("host=", "");
            port = scanner.nextLine().replace("port=", "");
            database = scanner.nextLine().replace("database=", "");
            username = scanner.nextLine().replace("user=", "");
            password = scanner.nextLine().replace("pass=", "");
        } catch (FileNotFoundException e) {
            System.out.println(STR."\{e.getMessage()} Could not find the file containing name and password for the database");
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

    /**
     * Checks if the correct database exists in the server. If the database doesn't exist, calls for
     * the next method to create a new database. If the database exists, calls for a method to check
     * if the database has a valid price list.
     * @return a valid price list back to the caller.
     */
    public static String checkIfDatabaseExists() {
        String sql = "SELECT EXISTS(SELECT datname FROM pg_catalog.pg_database WHERE datname = 'cosmosodyssey');";
        String priceListUuid = "";
        settings();
        try (Connection connection = DriverManager.getConnection("jdbc:postgresql://" + host + ":" + port + "/", username, password)) {
            String doesDatabaseExist = getDatabaseResponse(sql, connection);
            if (doesDatabaseExist.equals("f")) {
                priceListUuid = createNewDatabase();
            } else if (doesDatabaseExist.equals("t")) {
                priceListUuid = checkIfDatabaseHasValidPriceList();
            }
        } catch (SQLException | IOException e) {
            System.out.println(e.getMessage());
        }
        return priceListUuid;
    }

    /**
     * Creates a new database to the server if database doesn't exist. Then connects to the database and
     * creates tables for the database provided by the sql file. Calls for the APIReader to provide the
     * newest data and finally calls for the newest data to be placed into the database.
     * @return a valid price list back to the caller.
     */
    private static String createNewDatabase() {
        String createNewDatabase = "CREATE DATABASE CosmosOdyssey";
        String priceListUuid = "";
        try (Connection connection = DriverManager.getConnection("jdbc:postgresql://" + host + ":" + port + "/", username, password)) {
            Statement statement = connection.createStatement();
            statement.execute(createNewDatabase);
            try (Connection newConnection = connection()) {
                Path sqlPath = Path.of("Create SQL tables.sql");
                String sqlText = Files.readString(sqlPath);
                statement = newConnection.createStatement();
                statement.execute(sqlText);
                APIReader apiReader = new APIReader();
                JSONObject apiData = apiReader.getJsonDataFromAPI();
                SQLDatabaseTableWriter sqlDatabaseTableWriter = new PostgresTableWriter(newConnection);
                priceListUuid = sqlDatabaseTableWriter.insertDataToAllTables(apiData);
            } catch (SQLException | IOException e) {
                System.out.println(e.getMessage());
            }
        } catch (SQLException e) {
            System.out.println(STR."\{e.getMessage()} Could not connect to the server.");
        }
        return priceListUuid;
    }

    /**
     * Checks if the database has a valid price list. If no valid price list is found, calls
     * for a method to add a new price list.
     * @return a valid price list back to the caller.
     */
    private static String checkIfDatabaseHasValidPriceList() throws IOException {
        String sql = "SELECT uuid FROM price_list WHERE valid_until > NOW() - INTERVAL '3h';";
        String priceListUuid = "";
        try (Connection connection = connection()) {
            priceListUuid = getDatabaseResponse(sql, connection);
            if (priceListUuid.isEmpty()) {
                priceListUuid = addNewPriceList();
            }
        } catch (SQLException e) {
            System.out.println(STR."\{e.getMessage()} Could not connect to the server");
        }
        return priceListUuid;
    }

    /**
     * Calls for the APIReader to provide the newest data and then
     * calls for the newest data to be placed into the database.
     * @return a valid price list back to the caller.
     */
    private static String addNewPriceList() throws IOException {
        APIReader apiReader = new APIReader();
        JSONObject apiData = apiReader.getJsonDataFromAPI();
        String priceListUuid = "";
        try (Connection connection = connection()) {
            SQLDatabaseTableWriter sqlDatabaseTableWriter = new PostgresTableWriter(connection);
            priceListUuid = sqlDatabaseTableWriter.insertDataToTables(apiData);
        } catch (SQLException e) {
            System.out.println(STR."\{e.getMessage()} Could not connect to the server");
        }
        return priceListUuid;
    }
}
