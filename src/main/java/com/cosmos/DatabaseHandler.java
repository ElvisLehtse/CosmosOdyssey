package com.cosmos;

import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.sql.SQLException;

public interface DatabaseHandler {

    void insertPriceListToSQL(JSONObject apiData) throws SQLException, FileNotFoundException;
    void insertPlanetToSQL(JSONObject apiData) throws SQLException;
    void insertRouteInfoToSQL(JSONObject apiData) throws SQLException;
    void insertCompanyToSQL(JSONObject apiData) throws SQLException;
    void insertProviderToSQL(JSONObject apiData) throws SQLException;
}
