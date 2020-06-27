package main.clients;

/**
 * User clients
 */

public class Client_Android extends Client {
    private final String name; // Unique identifier for user clients
    private final boolean admin;
    // To identify a specific Android connection
    // even if the same user is logged in on multiple devices
    private static int connectionCounter = 0;
    private int connectionID;

    public Client_Android(int homeServerID, Client oldClient, String name, boolean admin) {
        super(homeServerID, oldClient.getSocket(), oldClient.getInput(), oldClient.getOutput());
        this.name = name;
        this.admin = admin;
        connectionID = ++connectionCounter;
        crypto = oldClient.crypto;
    }

    public String getName() {
        return name;
    }

    public boolean isAdmin() {
        return admin;
    }

    public int getConnectionID() {
        return connectionID;
    }

}
