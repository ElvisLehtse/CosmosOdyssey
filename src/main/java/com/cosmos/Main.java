package com.cosmos;

import com.cosmos.SQL.postgres.PostgresDatabaseConnector;
import java.io.IOException;

public class Main {

    public static void main(String[] args) {
        PostgresDatabaseConnector postgresDatabaseConnector = new PostgresDatabaseConnector();

        try {
            postgresDatabaseConnector.checkIfDatabaseExists();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}