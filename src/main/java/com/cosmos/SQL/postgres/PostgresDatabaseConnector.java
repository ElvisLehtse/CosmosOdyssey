package com.cosmos.SQL.postgres;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class PostgresDatabaseConnector {

    private final String host;
    private final String port;
    private final String username;
    private final String password;
    private final String database;

    public PostgresDatabaseConnector (String host, String port, String database, String username, String password) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
    }

    public Connection connection() throws SQLException {
        return DriverManager.getConnection("jdbc:postgresql://" + host + ":" + port + "/" + database, username, password);
    }
}
