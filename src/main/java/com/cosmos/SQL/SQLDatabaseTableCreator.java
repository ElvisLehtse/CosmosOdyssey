package com.cosmos.SQL;

import org.json.JSONObject;
import java.sql.SQLException;

public interface SQLDatabaseTableCreator {

    void createAllTables(JSONObject apiData) throws SQLException;
}
