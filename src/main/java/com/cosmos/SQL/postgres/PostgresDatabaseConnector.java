package com.cosmos.SQL.postgres;

import com.cosmos.APIReader;
import com.cosmos.SQL.SQLDatabaseTableCreator;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.*;
import java.util.List;
import java.util.Scanner;

public class PostgresDatabaseConnector {

    private String host;
    private String port;
    private String database;
    private String username;
    private String password;
    private List<String> userDefinedCompanyNames;
    private String originPlanet;
    private String destinationPlanet;

    public Connection connection() throws SQLException {
        return DriverManager.getConnection("jdbc:postgresql://" + host + ":" + port + "/" + database, username, password);
    }
    private void settings(int databaseIndex) {
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

    public void checkIfDatabaseExists(List<String> userDefinedCompanyNames, String originPlanet, String destinationPlanet) throws IOException {
        this.userDefinedCompanyNames = userDefinedCompanyNames;
        this.originPlanet = originPlanet;
        this.destinationPlanet = destinationPlanet;
        boolean doesDatabaseExist = true;
        int databaseIndex = 0;
        while (doesDatabaseExist) {
            databaseIndex++;
            String checkIfDatabaseExistsSQL =
                    STR."SELECT EXISTS(SELECT datname FROM pg_catalog.pg_database WHERE datname = 'cosmosodyssey\{databaseIndex}');";
            settings(databaseIndex);
            try (Connection connection = DriverManager.getConnection("jdbc:postgresql://" + host + ":" + port + "/", username, password)) {
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(checkIfDatabaseExistsSQL);
                resultSet.next();
                String databaseStatus = resultSet.getString(1);
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

    private void checkDatabaseDate(int databaseIndex) throws IOException {
        String checkDatabaseValidUntil = "SELECT valid_until FROM price_list;";
        settings(databaseIndex);
        try (Connection connection = connection()) {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(checkDatabaseValidUntil);
            resultSet.next();
            String databaseTime = resultSet.getString(1);
            String currentTime = (new Timestamp(System.currentTimeMillis())).toString();
            int isDatabaseValid = databaseTime.compareTo(currentTime);
            //if (isDatabaseValid > 0) { // CORRECT SOLUTION! Temporarily changed
            if (isDatabaseValid < 0) {
                InitiateCalculator initiateCalculator = new InitiateCalculator(connection);
                initiateCalculator.runCalculator(userDefinedCompanyNames, originPlanet, destinationPlanet);
            } else {
                databaseIndex++;
                checkIfApiHasValidDate(databaseIndex);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    private void checkIfApiHasValidDate(int databaseIndex) throws IOException {
        APIReader apiReader = new APIReader();
        JSONObject apiData = apiReader.getJsonDataFromAPI();
        String apiValidUntil = apiData.getString("validUntil");
        String apiValidUntilFormatted = apiValidUntil.replace("T", " ").replace("Z", "");
        String currentTime = (new Timestamp(System.currentTimeMillis())).toString();
        int isApiValid = apiValidUntilFormatted.compareTo(currentTime);
       // if (isApiValid > 0) { // CORRECT SOLUTION! Temporarily changed
        if (isApiValid < 0) {
            createNewDatabase(databaseIndex);
        } else {
            // Do something here SERVER response:
            System.out.println("There are currently no available price lists");
        }
    }

    private void createNewDatabase(int databaseIndex) {
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

                InitiateCalculator initiateCalculator = new InitiateCalculator(newConnection);
                initiateCalculator.runCalculator(userDefinedCompanyNames, originPlanet, destinationPlanet);
            } catch (SQLException | IOException e) {
                System.out.println(e.getMessage());
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
}
