package com.cosmos;

import com.cosmos.SQL.postgres.PostgresDatabaseConnector;
import com.cosmos.SQL.postgres.initiator.ReadValidDataFromDatabase;
import com.cosmos.server.ServerStarter;

import java.sql.SQLException;

/**
 * Localhost version of the project.
 * The main method initiates the creation or the update of the postgreSQL database,
 * initiates the reading of valid data from the database and starts the localhost server.
 */
public class Main {

    public static void main(String[] args) {
        try {
            String priceListUuid = PostgresDatabaseConnector.checkIfDatabaseExists();
            new ReadValidDataFromDatabase(PostgresDatabaseConnector.connection(), priceListUuid);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        ServerStarter serverStarter = new ServerStarter();
        serverStarter.startServer();
    }
}