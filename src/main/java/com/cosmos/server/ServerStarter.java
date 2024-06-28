package com.cosmos.server;

import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Scanner;

/**
 * This class starts the localhost server.
 */
public class ServerStarter {

    public void startServer() {
        File file = new File("Localhost port.txt");
        try {
            Scanner scanner = new Scanner(file);
            int port = Integer.parseInt(scanner.nextLine().replace("port=", ""));
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
            RequestHandler requestHandler = new RequestHandler();
            requestHandler.requestGetAndPost(server, "/");
            server.start();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}
