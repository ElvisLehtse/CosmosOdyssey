package com.cosmos;

import com.cosmos.SQL.postgres.BestDealCalculator;
import com.cosmos.SQL.postgres.PostgresDatabaseConnector;
import org.json.JSONObject;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {
    static String host;
    static String port;
    static String database;
    static String username;
    static String password;
    public static void main(String[] args) {
        settings();
        PostgresDatabaseConnector postgresDatabaseConnector = new PostgresDatabaseConnector(host, port, database, username, password);
        APIReader routesManager = new APIReader();
        List<String> companyList = new ArrayList<>();
        companyList.add("27cb4c5c-04ba-4d41-ba16-013c504e0c2e");
        /*
        companyList.add("c693f50a-f9e4-47b7-a221-c867873f18c5");
        companyList.add("f470e765-1691-4176-a859-1732217adb1d");
        companyList.add("f1f8b93e-e390-4e84-b4ce-aae5cd7036f3");
        companyList.add("b176b51f-77bf-46cd-8181-1bb3b86cf523");
        companyList.add("5f4ccd14-67b1-4f4f-8006-a50e5c773a9f");
        companyList.add("6b92b80d-2a1c-4462-b70a-148f2ca05e2e");
        companyList.add("f6c7fe92-ec36-40bb-afe7-52e39f8dc217");
        companyList.add("b2ab674c-5881-4751-97d0-ed37967c736c");
        companyList.add("7688b63e-0ff0-4cfd-9599-17f712da9ca4");

         */
        try (Connection connection = postgresDatabaseConnector.connection()){
            JSONObject apiData = routesManager.getJsonDataFromAPI();
       //     SQLDatabaseTableCreator sqlDatabaseTableCreator = new PostgresTableCreator(connection);
       //     sqlDatabaseTableCreator.createAllTables(apiData);
            BestDealCalculator bestDealCalculator = new BestDealCalculator(connection);
            bestDealCalculator.generateSolutions("61adebf4-387a-4035-bdbb-12999d3673bf", "eaf7cff3-3e58-414e-9ecb-a6acd428ed2d", companyList);

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