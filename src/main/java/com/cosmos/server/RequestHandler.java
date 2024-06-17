package com.cosmos.server;

import com.cosmos.SQL.postgres.PostgresDatabaseConnector;
import com.cosmos.SQL.postgres.initiator.Company;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class RequestHandler {

    private String mainPageReader() throws IOException {
        Path mainPageFilePath = Path.of("Mainpage.html");
        return Files.readString(mainPageFilePath);
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

    public void requestGetAndPost (HttpServer server, String requestPath) {

        server.createContext(requestPath, (HttpExchange exchange) ->
        {
            String reply = "";
            try {
                if (exchange.getRequestMethod().equals("GET")) {
                    reply = mainPageReader();
                } else if (exchange.getRequestMethod().equals("POST")) {
                    BufferedReader requestBody = requestBodyMsg(exchange);

                    List<String> userDefinedCompanyNames = new ArrayList<>();
                    String originPlanet = "";
                    String destinationPlanet = "";
                    String query = requestBody.readLine();
                    String[] params = query.split("&");
                    for (String  param : params) {
                        String[] keyValue = param.split("=");
                        String name = keyValue[0];
                        String value = keyValue[1];
                        if (name.equals("companies")) {
                            userDefinedCompanyNames.add(value.replace("+", " "));
                        } else if (name.equals("originplanet")) {
                            originPlanet = value;
                        } else if (name.equals("destinationplanet")) {
                            destinationPlanet = value;
                        }
                    }

                    PostgresDatabaseConnector postgresDatabaseConnector = new PostgresDatabaseConnector();
                    try {
                        postgresDatabaseConnector.checkIfDatabaseExists(userDefinedCompanyNames, originPlanet, destinationPlanet);
                    } catch (IOException e) {
                        System.out.println(e.getMessage());
                    }

                    reply = "<html>" +
                            "   <body>" +
                            "       <p> " +
                            "           <br>All possible routes from " + originPlanet + " to " + destinationPlanet + ":" +
                            "           <br>" + path +
                            "           <br>When traveling with companies:</br>" +
                            "           <br>" + companies +
                            "       </p>" +
                            "   </body>" +
                            "</html>";
                }
            } catch (IOException e) {
                System.out.println(STR."\{e.getMessage()} Could not access the file");
                reply = "Could not access the file";
            } finally {
                serverResponse(exchange, reply);
            }
        });
    }

    private static String path;
    private static String companies;

    public static void setPath(String path) {
        RequestHandler.path = path;
    }
    public static void setCompanies(String companies) {
        RequestHandler.companies = companies;
    }
}
