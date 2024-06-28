package com.cosmos.server;

import com.cosmos.SQL.postgres.BestDealCalculator;
import com.cosmos.SQL.postgres.PostgresDatabaseConnector;
import com.cosmos.SQL.postgres.PostgresTableWriter;
import com.cosmos.SQL.postgres.initiator.Provider;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * This class handles get and post requests
 */
public class RequestHandler {
    private static String path = "";
    private static List<List<Provider>> sortedListForGettingUserChoice;
    private String originPlanet = "";
    private String destinationPlanet = "";
    private int routeNumber;

    public static void setPath(String path) {
        RequestHandler.path = path;
    }
    public static void setSortedListForGettingUserChoice(List<List<Provider>> sortedListForGettingUserChoice) {
        RequestHandler.sortedListForGettingUserChoice = sortedListForGettingUserChoice;
    }

    /**
     * Reads the front-end code from the html file.
     * @return it as a string.
     */
    private String htmlReader() throws IOException {
        Path hmtlPath = Path.of("CosmosOdyssey.html");
        return Files.readString(hmtlPath);
    }

    private BufferedReader requestBodyMsg (HttpExchange exchange) {
        InputStream inputStream = exchange.getRequestBody();
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
        return new BufferedReader(inputStreamReader);
    }

    /**
     * Provides a response message to the client.
     */
    private void serverResponse (HttpExchange exchange, String reply) {
        try {
            exchange.sendResponseHeaders(200, reply.length());
            OutputStream output = exchange.getResponseBody();
            output.write(reply.getBytes());
            output.flush();
            output.close();
            exchange.close();
        } catch (IOException e) {
            System.out.println(STR."\{e.getMessage()} Could not send reply to client");
        }
    }

    /**
     * Configures the response message for the client.
     */
    private String htmlMessageConfigurator(String planet, String error, String result, String register, String success) throws IOException {
        String htmlString = htmlReader();
        htmlString = htmlString.replace("$originPlanet", originPlanet);
        htmlString = htmlString.replace("$destinationPlanet", STR."\{destinationPlanet}:");
        htmlString = htmlString.replace("$path", path);
        htmlString = htmlString.replace("displayOfPlanetSelection", planet);
        htmlString = htmlString.replace("displayOfErrorMessage", error);
        htmlString = htmlString.replace("displayOfResultSelection", result);
        htmlString = htmlString.replace("displayOfRegisterSelection", register);
        htmlString = htmlString.replace("displaySuccessfulRegistration", success);
        return htmlString;
    }

    /**
     * This method handles the GET and POST requests from the client.
     */
    public void requestGetAndPost (HttpServer server, String requestPath) {
        server.createContext(requestPath, (HttpExchange exchange) ->
        {
            String reply = "";
            try {
                if (exchange.getRequestMethod().equals("GET")) {
                    reply = htmlMessageConfigurator("unset", "none", "none", "none", "none");
                } else if (exchange.getRequestMethod().equals("POST")) {
                    BufferedReader requestBody = requestBodyMsg(exchange);
                    String query = requestBody.readLine();
                    reply = filterQueryParams(query);
                }
            } catch (IOException | SQLException e) {
                System.out.println(STR."\{e.getMessage()} Could not access the file");
                reply = "A problem occurred with the server.";
            } finally {
                serverResponse(exchange, reply);
            }
        });
    }

    /**
     * Filters the query information from the POST request. Depending on the query information,
     * values are stored and proper response messages are modified before returning a reply message.
     */
    BestDealCalculator bestDealCalculator = new BestDealCalculator();
    private String filterQueryParams(String query) throws IOException, SQLException {
        List<String> userDefinedCompanyNames = new ArrayList<>();
        String initialFiltering = "price";
        String filterBy = "";
        String firstName = "";
        String lastName = "";
        String htmlString = "";
        int postRequestNumber = 0;
        String[] params = query.split("&");
        for (String  param : params) {
            String[] keyValue = param.split("=");
            String name = keyValue[0];
            String value = keyValue[1];
            switch (name) {
                case "originplanet" -> originPlanet = value;
                case "destinationplanet" -> {
                    destinationPlanet = value;
                    filterBy = initialFiltering;
                    postRequestNumber = 1;
                    if (originPlanet.equals(destinationPlanet)) {
                        postRequestNumber = 6;
                    }
                }
                case "companies" -> {
                    userDefinedCompanyNames.add(value.replace("+", " "));
                    postRequestNumber = 2;
                }
                case "routeName" -> {
                    routeNumber = Integer.parseInt(value);
                    postRequestNumber = 3;
                }
                case "firstname" -> firstName = value;
                case "lastname" -> lastName = value;
                case "register" -> postRequestNumber = 4;
                case "options" -> {
                    filterBy = value;
                    if (userDefinedCompanyNames.isEmpty()) {
                        postRequestNumber = 1;
                    } else {
                        postRequestNumber = 2;
                    }
                }
                default -> postRequestNumber = 5;
            }
        }
        switch (postRequestNumber) {
            case 1 -> {
                bestDealCalculator.generateUnfilteredSolutions(originPlanet, destinationPlanet, filterBy);
                htmlString = htmlMessageConfigurator("unset", "none", "unset", "none", "none");
            }

            case 2 -> {
                bestDealCalculator.generateSolutions(originPlanet, destinationPlanet, userDefinedCompanyNames, filterBy);
                htmlString = htmlMessageConfigurator("unset", "none", "unset", "none", "none");
            }
            case 3 -> htmlString = htmlMessageConfigurator("none", "none", "none", "unset", "none");
            case 4 -> {
                Connection connection = PostgresDatabaseConnector.connection();
                PostgresTableWriter postgresTableWriter = new PostgresTableWriter(connection);
                postgresTableWriter.storeUserChoice(sortedListForGettingUserChoice, routeNumber, firstName, lastName);
                htmlString = htmlMessageConfigurator("none", "none", "none", "none", "unset");
            }
            case 5 -> htmlString = htmlMessageConfigurator("unset", "none", "none", "none", "none");
            case 6 -> htmlString = htmlMessageConfigurator("unset", "unset", "none", "none", "none");
        }
        return htmlString;
    }
}
