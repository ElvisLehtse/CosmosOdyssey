package com.cosmos.server;

import com.cosmos.SQL.postgres.BestDealCalculator;
import com.cosmos.SQL.postgres.PostgresDatabaseConnector;
import com.cosmos.SQL.postgres.PostgresTableCreator;
import com.cosmos.SQL.postgres.initiator.Provider;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RequestHandler {

    private String planetSelectionReader() throws IOException {
        Path mainPageFilePath = Path.of("PlanetSelection.html");
        return Files.readString(mainPageFilePath);
    }

    private String filteredRoutesReader() throws IOException {
        Path registerFilePath = Path.of("FilteredRoutes.html");
        return Files.readString(registerFilePath);
    }

    private String registerReader() throws IOException {
        Path registerFilePath = Path.of("Register.html");
        return Files.readString(registerFilePath);
    }

    private BufferedReader requestBodyMsg (HttpExchange exchange) {
        InputStream inputStream = exchange.getRequestBody();
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
        return new BufferedReader(inputStreamReader);
    }

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

    private static String path;
    private static List<List<Provider>> sortedListForGettingUserChoice;
    private String originPlanet;
    private String destinationPlanet;
    private int routeNumber;

    public static void setPath(String path) {
        RequestHandler.path = path;
    }
    public static void setSortedListForGettingUserChoice(List<List<Provider>> sortedListForGettingUserChoice) {
        RequestHandler.sortedListForGettingUserChoice = sortedListForGettingUserChoice;
    }

    public void requestGetAndPost (HttpServer server, String requestPath) {

        server.createContext(requestPath, (HttpExchange exchange) ->
        {
            String reply = "";
            try {
                if (exchange.getRequestMethod().equals("GET")) {
                    reply = planetSelectionReader();
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

    BestDealCalculator bestDealCalculator = new BestDealCalculator();
    private String filterQueryParams(String query) throws IOException, SQLException {
        List<String> userDefinedCompanyNames = new ArrayList<>();
        String initialFiltering = "price";
        String filterBy = "";
        String firstName = "";
        String lastName = "";
        int postRequestNumber = 0;
        String[] params = query.split("&");
        for (String  param : params) {
            String[] keyValue = param.split("=");
            String name = keyValue[0];
            String value = keyValue[1];
            switch (name) {
                case "originplanet" -> {
                    originPlanet = value;
                    filterBy = initialFiltering;
                    postRequestNumber = 1;
                }
                case "destinationplanet" -> destinationPlanet = value;
                case "companies" -> {
                    userDefinedCompanyNames.add(value.replace("+", " "));
                    postRequestNumber = 2;
                }
                case "routeName" -> {
                    routeNumber = Integer.parseInt(value);
                    postRequestNumber = 3;
                }
                case "firstname" -> {
                    firstName = value;
                    postRequestNumber = 4;
                }
                case "lastname" -> lastName = value;
                case "options" -> {
                    filterBy = value;
                    if (userDefinedCompanyNames.isEmpty()) {
                        postRequestNumber = 1;
                    } else {
                        postRequestNumber = 2;
                    }

                }
            }
        }
        String htmlString = "";
        switch (postRequestNumber) {
            case 1 -> {
                bestDealCalculator.generateUnfilteredSolutions(originPlanet, destinationPlanet, filterBy);
                htmlString = filteredRoutesReader();
                htmlString = htmlString.replace("$originPlanet", originPlanet);
                htmlString = htmlString.replace("$destinationPlanet", STR."\{destinationPlanet}:");
                htmlString = htmlString.replace("$path", path);
            }
            case 2 -> {
                bestDealCalculator.generateSolutions(originPlanet, destinationPlanet, userDefinedCompanyNames, filterBy);
                htmlString = filteredRoutesReader();
                htmlString = htmlString.replace("$originPlanet", originPlanet);
                htmlString = htmlString.replace("$destinationPlanet", STR."\{destinationPlanet}:");
                htmlString = htmlString.replace("$path", path);
            }
            case 3 -> {
                htmlString = registerReader();
            }

            case 4 -> {
                Connection connection = PostgresDatabaseConnector.connection();
                PostgresTableCreator postgresTableCreator = new PostgresTableCreator(connection);
                postgresTableCreator.storeUserChoice(sortedListForGettingUserChoice, routeNumber, firstName, lastName);
                htmlString = "<html><body><span>Your choice has been registered.</span></body></html>";
            }
        }
        return htmlString;
    }

   /* private void secondPostRequest() {
        List<String> userDefinedCompanyNames = new ArrayList<>();
        String originPlanet = null;
        String destinationPlanet;



            switch (name) {
                case "companies" -> userDefinedCompanyNames.add(value.replace("+", " "));
                case "originplanet" -> originPlanet = value;
                case "destinationplanet" -> {
                    destinationPlanet = value;
                    PostgresDatabaseConnector postgresDatabaseConnector = new PostgresDatabaseConnector();
                    try {
                        postgresDatabaseConnector.checkIfDatabaseExists(originPlanet, destinationPlanet);
                    } catch (IOException e) {
                        System.out.println(e.getMessage());
                    }
                    reply = "<html>" +
                            "   <body>" +
                            "       <p>" +
                            "           All possible routes from " + originPlanet + " to " + destinationPlanet + ":<br>" +
                            path + "<br>" +
                            "           When traveling with companies:<br>" +
                            companies +
                            "       </p>" +
                            registerReader() +
                            "   </body>" +
                            "</html>";
                }
                case "routename" -> routeName = Integer.parseInt(value);
                case "fistname" -> firstName = value;
                case "lastname" -> {
                    lastName = value;
                    reply = "You have been registred!";
                }
            }
        }
    }*/



}
