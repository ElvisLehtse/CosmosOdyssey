package com.cosmos;

import com.cosmos.SQL.postgres.PostgresDatabaseConnector;
import com.cosmos.SQL.postgres.initiator.InitiateLists;
import com.cosmos.server.ServerStarter;

import java.io.IOException;
import java.sql.SQLException;

public class Main {

    public static void main(String[] args) {
        try {
            String priceListUuid = PostgresDatabaseConnector.checkIfDatabaseExists();
            new InitiateLists(PostgresDatabaseConnector.connection(), priceListUuid);
        } catch (IOException | SQLException e) {
            System.out.println(e.getMessage());
        }
        ServerStarter serverStarter = new ServerStarter();
        serverStarter.startServer();
    }
}