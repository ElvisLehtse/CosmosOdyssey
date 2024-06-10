package com.cosmos;

import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Scanner;

public class Main {
    static String host;
    static String port;
    static String database;
    static String username;
    static String password;
    public static void main(String[] args) {
        settings();
        DatabaseHandler databaseHandler = new PostgresWriter(host, port, database, username, password);
        APIReader routesManager = new APIReader();
        try {
            JSONObject apiData = routesManager.getJsonDataFromAPI();
            databaseHandler.insertPriceListToSQL(apiData);
            databaseHandler.insertPlanetToSQL(apiData);
            databaseHandler.insertRouteInfoToSQL(apiData);
            databaseHandler.insertCompanyToSQL(apiData);
            databaseHandler.insertProviderToSQL(apiData);
        } catch (IOException | SQLException e) {
            System.out.println(e.getMessage());
        }

    }
    public static void settings() {
        File file = new File("Postgres credentials.txt");
        try {
            Scanner scanner = new Scanner(file);
            host = "localhost";
            port = "5432";
            database = "CosmosOdyssey";
            username = scanner.nextLine().replace("user=", "");
            password = scanner.nextLine().replace("pass=", "");
        } catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
        }

    }
}