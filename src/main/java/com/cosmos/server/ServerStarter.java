package com.cosmos.server;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;

public class ServerStarter {

    public void startServer() {
        final int port = 8080;
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
            RequestHandler requestHandler = new RequestHandler();
            requestHandler.requestGetAndPost(server, "/");
            server.start();
        } catch (IOException e) {
            System.out.println(STR."\{e.getMessage()} Could not create the server");
        }
    }
}
