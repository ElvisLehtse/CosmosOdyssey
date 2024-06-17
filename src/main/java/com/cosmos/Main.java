package com.cosmos;

import com.cosmos.server.ServerStarter;

public class Main {

    public static void main(String[] args) {
        ServerStarter serverStarter = new ServerStarter();
        serverStarter.startServer();
    }
}