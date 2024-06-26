package com.cosmos.SQL.postgres.initiator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class InitiateLists {

    private Connection connection = null;

    public InitiateLists() {}
    public InitiateLists(Connection connection, String priceListUuid) throws SQLException {
        this.connection = connection;
        initiatePlanets();
        initiateRoutes(priceListUuid);
        initiateCompany(priceListUuid);
        initiateProviders(priceListUuid);
    }

    private static List<RouteInfo> routeList = new ArrayList<>();
    private static List<Planet> planetList = new ArrayList<>();
    private static List<Provider> providerList = new ArrayList<>();
    private static List<Company> companyList = new ArrayList<>();

    public List<RouteInfo> getRouteList() {
        return routeList;
    }

    public List<Planet> getPlanetList() {
        return planetList;
    }

    public List<Provider> getProviderList() {
        return providerList;
    }

    public List<Company> getCompanyList() {
        return companyList;
    }

    private void initiatePlanets() throws SQLException {
        planetList = new ArrayList<>();
        String sql= "SELECT * FROM planet;";
        PreparedStatement readStatement = connection.prepareStatement(sql);
        ResultSet resultSet = readStatement.executeQuery();

        while (resultSet.next()) {
            planetList.add(new Planet(resultSet.getString(1), resultSet.getString(2)));
        }
    }

    private void initiateRoutes(String priceListUuid) throws SQLException {
        routeList = new ArrayList<>();
        String sql= STR."SELECT * FROM route_info WHERE price_list_uuid = '\{priceListUuid}';";
        PreparedStatement readStatement = connection.prepareStatement(sql);
        ResultSet resultSet = readStatement.executeQuery();

        while (resultSet.next()) {
            routeList.add(new RouteInfo(resultSet.getString(1), resultSet.getString(2),
                    resultSet.getString(3), resultSet.getString(4), resultSet.getLong(5)));
        }
    }

    private void initiateProviders(String priceListUuid) throws SQLException {
        providerList = new ArrayList<>();
        String sql= "SELECT provider.uuid, company_uuid, route_info_uuid, price, flight_start, flight_end FROM provider " +
                "INNER JOIN route_info ON route_info.uuid = provider.route_info_uuid WHERE price_list_uuid = '" + priceListUuid + "';";
        PreparedStatement readStatement = connection.prepareStatement(sql);
        ResultSet resultSet = readStatement.executeQuery();

        while (resultSet.next()) {
            providerList.add(new Provider(resultSet.getString(1), resultSet.getString(2), resultSet.getString(3),
                    resultSet.getLong(4), resultSet.getTimestamp(5), resultSet.getTimestamp(6)));
        }
    }

    private void initiateCompany(String priceListUuid) throws SQLException {
        companyList = new ArrayList<>();
        String sql= "SELECT DISTINCT company.uuid, name FROM company INNER JOIN provider " +
                "ON company.uuid = provider.company_uuid INNER JOIN route_info " +
                "ON provider.route_info_uuid = route_info.uuid WHERE price_list_uuid = '" + priceListUuid +"';";


        PreparedStatement readStatement = connection.prepareStatement(sql);
        ResultSet resultSet = readStatement.executeQuery();

        while (resultSet.next()) {
            companyList.add(new Company(resultSet.getString(1), resultSet.getString(2)));
        }
    }
}
