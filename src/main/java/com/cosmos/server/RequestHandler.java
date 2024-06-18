package com.cosmos.server;

import com.cosmos.SQL.postgres.BestDealCalculator;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RequestHandler {

    private String planetSelectionReader() throws IOException {
        Path mainPageFilePath = Path.of("PlanetSelection.html");
        return Files.readString(mainPageFilePath);
    }

    private String routesAndFilterReader() throws IOException {
        Path registerFilePath = Path.of("RoutesAndFilter.html");
        return Files.readString(registerFilePath);
    }

    private String filteredRoutesReader() throws IOException {
        Path registerFilePath = Path.of("FilteredRoutes.html");
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

    private static String cheapestPath;
    private static String fastestPath;
    private static String cheapestCompanies;
    private static String fastestCompanies;

    public static void setCheapestPath(String path) {
        RequestHandler.cheapestPath = path;
    }
    public static void setFastestPath(String path) {
        RequestHandler.fastestPath = path;
    }
    public static void setCheapestCompanies(String companies) {
        RequestHandler.cheapestCompanies = companies;
    }
    public static void setFastestCompanies(String companies) {
        RequestHandler.fastestCompanies = companies;
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
    String originPlanet;
    String destinationPlanet;

    BestDealCalculator bestDealCalculator = new BestDealCalculator();
    private String filterQueryParams(String query) throws IOException, SQLException {
        List<String> userDefinedCompanyNames = new ArrayList<>();

        String[] params = query.split("&");
        for (String  param : params) {
            String[] keyValue = param.split("=");
            String name = keyValue[0];
            String value = keyValue[1];
            switch (name) {
                case "originplanet" -> originPlanet = value;
                case "destinationplanet" -> destinationPlanet = value;
                case "companies" -> userDefinedCompanyNames.add(value.replace("+", " "));
            }
        }
        String htmlString = "";
        if (userDefinedCompanyNames.isEmpty()) {
            bestDealCalculator.generateSolutions(originPlanet, destinationPlanet);
            htmlString = routesAndFilterReader();
            htmlString = htmlString.replace("$originPlanet", originPlanet);
            htmlString = htmlString.replace("$destinationPlanet", destinationPlanet + ":");
            htmlString = htmlString.replace("$cheapestPath", cheapestPath);
            htmlString = htmlString.replace("$fastestPath", fastestPath);

        } else {
            bestDealCalculator.generateFilteredSolutions(originPlanet, destinationPlanet, userDefinedCompanyNames);
            htmlString = filteredRoutesReader();
            htmlString = htmlString.replace("$originPlanet", originPlanet);
            htmlString = htmlString.replace("$destinationPlanet", destinationPlanet + ":");
            htmlString = htmlString.replace("$cheapestPath", cheapestPath);
            htmlString = htmlString.replace("$fastestPath", fastestPath);
            htmlString = htmlString.replace("$cheapestCompanies", cheapestCompanies);
            htmlString = htmlString.replace("$fastestCompanies", fastestCompanies);
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
