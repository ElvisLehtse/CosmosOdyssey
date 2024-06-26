package com.cosmos.SQL;

import org.json.JSONObject;
import java.sql.SQLException;

public interface SQLDatabaseTableCreator {

    String createAllTables(JSONObject apiData) throws SQLException;
    String createTables(JSONObject apiData) throws SQLException;
}
