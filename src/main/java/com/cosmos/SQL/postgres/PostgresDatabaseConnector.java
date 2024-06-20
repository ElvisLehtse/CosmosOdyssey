package com.cosmos.SQL.postgres;

import com.cosmos.APIReader;
import com.cosmos.SQL.SQLDatabaseTableCreator;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.*;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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
    private static void settings(int databaseIndex) {
        File file = new File("Postgres credentials.txt");
        try {
            Scanner scanner = new Scanner(file);
            host = "localhost";
            port = "5432";
            database = STR."cosmosodyssey\{databaseIndex}";
            username = scanner.nextLine().replace("user=", "");
            password = scanner.nextLine().replace("pass=", "");
        } catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
        }
    }

    private static String getDatabaseResponse(String sql, Connection connection) throws SQLException {
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery(sql);
        resultSet.next();
        return resultSet.getString(1);
    }

    public static void checkIfDatabaseExists() throws IOException {
        boolean doesDatabaseExist = true;
        int databaseIndex = 0;
        while (doesDatabaseExist) {
            databaseIndex++;
            String sql =
                    STR."SELECT EXISTS(SELECT datname FROM pg_catalog.pg_database WHERE datname = 'cosmosodyssey\{databaseIndex}');";
            settings(databaseIndex);
            try (Connection connection = DriverManager.getConnection("jdbc:postgresql://" + host + ":" + port + "/", username, password)) {
                String databaseStatus = getDatabaseResponse(sql, connection);
                if (databaseStatus.equals("f")) {
                    doesDatabaseExist = false;
                    if (databaseIndex == 1) {
                        checkIfApiHasValidDate(databaseIndex);
                    } else {
                        databaseIndex--;
                        checkDatabaseDate(databaseIndex);
                    }
                }
            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private static void checkDatabaseDate(int databaseIndex) throws IOException {
        String sql = "SELECT valid_until FROM price_list;";
        settings(databaseIndex);
        try (Connection connection = connection()) {
            String databaseTime = getDatabaseResponse(sql, connection);
            ZonedDateTime zonedDateTime = ZonedDateTime.now(ZoneId.of("GMT"));
            String currentTime = Timestamp.valueOf(zonedDateTime.toLocalDateTime()).toString();
            //int isDatabaseValid = databaseTime.compareTo(currentTime);
            int isDatabaseValid = 1;
            if (isDatabaseValid < 0) {
                databaseIndex++;
                checkIfApiHasValidDate(databaseIndex);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    private static void checkIfApiHasValidDate(int databaseIndex) throws IOException {
        APIReader apiReader = new APIReader();
        JSONObject apiData = apiReader.getJsonDataFromAPI();
        String apiValidUntil = apiData.getString("validUntil");
        String apiValidUntilFormatted = apiValidUntil.replace("T", " ").replace("Z", "");
        ZonedDateTime zonedDateTime = ZonedDateTime.now(ZoneId.of("GMT"));
        String currentTime = Timestamp.valueOf(zonedDateTime.toLocalDateTime()).toString();
        int isApiValid = apiValidUntilFormatted.compareTo(currentTime);
        if (isApiValid > 0) {
            createNewDatabase(databaseIndex);
        } else {
            // Do something here SERVER response:
            System.out.println("There are currently no available price lists");
        }
    }

    private static void createNewDatabase(int databaseIndex) {
        String createNewDatabase = STR."CREATE DATABASE CosmosOdyssey\{databaseIndex}";
        settings(databaseIndex);
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
                sqlDatabaseTableCreator.createAllTables(apiData);
            } catch (SQLException | IOException e) {
                System.out.println(e.getMessage());
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
}
