package main.clients;

/**
 * Home Servers a.k.a. hub clients.
 * E.g. realized by running as a Service on Raspberry Pi in local network; controlling gadgets
 */

public class Client_HomeServer extends Client {
    private String alias;

    public Client_HomeServer(int homeServerID, Client oldClient, String alias) {
        super(homeServerID, oldClient.getSocket(), oldClient.getInput(), oldClient.getOutput());
        this.alias = alias;
        crypto = oldClient.crypto;
    }

    public String getAlias() {
        return alias;
    }

}
