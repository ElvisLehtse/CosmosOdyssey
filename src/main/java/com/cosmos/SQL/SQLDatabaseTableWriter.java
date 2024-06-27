package com.cosmos.SQL;

import org.json.JSONObject;
import java.sql.SQLException;

public interface SQLDatabaseTableWriter {

    String insertDataToAllTables(JSONObject apiData) throws SQLException;
    String insertDataToTables(JSONObject apiData) throws SQLException;
}
