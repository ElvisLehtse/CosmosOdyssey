package com.cosmos.SQL.postgres;

import com.cosmos.SQL.postgres.initiator.Company;
import com.cosmos.SQL.postgres.initiator.InitiateLists;
import com.cosmos.SQL.postgres.initiator.Planet;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class InitiateCalculator {

    public InitiateCalculator (Connection connection) throws SQLException {
        InitiateLists initiateLists = new InitiateLists(connection);
        companyList = initiateLists.getCompanyList();
        planetList = initiateLists.getPlanetList();
        this.connection = connection;
    }
    static List<Company> companyList = new ArrayList<>();
    static List<Planet> planetList = new ArrayList<>();
    private final Connection connection;

    private List<String> companySetup(List<String> userDefinedCompanyNames) {
        List<String> listOfDefinedCompanyUuid = new ArrayList<>();
        for (String selectedCompanies : userDefinedCompanyNames) {
            for (Company company : companyList) {
                if (selectedCompanies.equals(company.getName())) {
                    listOfDefinedCompanyUuid.add(company.getUuid());
                }
            }
        }
        return listOfDefinedCompanyUuid;
    }

    private List<String> planetSetup(String originPlanet, String destinationPlanet) {
        List<String> userDefinedPlanetNames = new ArrayList<>();
        userDefinedPlanetNames.add(originPlanet);
        userDefinedPlanetNames.add(destinationPlanet);
        List<String> listOfDefinedPlanetUuid = new ArrayList<>();
        for (String selectedPlanets : userDefinedPlanetNames) {
            for (Planet planet : planetList) {
                if (selectedPlanets.equals(planet.getName())) {
                    listOfDefinedPlanetUuid.add(planet.getUuid());
                }
            }
        }
        return listOfDefinedPlanetUuid;
    }

    public void runCalculator(List<String> userDefinedCompanyNames, String originPlanet, String destinationPlanet) {
        List<String> listOfDefinedCompanyUuid = companySetup(userDefinedCompanyNames);
        List<String> listOfDefinedPlanetUuid = planetSetup(originPlanet, destinationPlanet);
        try (connection){
            BestDealCalculator bestDealCalculator = new BestDealCalculator(connection);
            bestDealCalculator.generateSolutions(listOfDefinedPlanetUuid.getFirst(), listOfDefinedPlanetUuid.getLast(), listOfDefinedCompanyUuid);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
}
