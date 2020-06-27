package main.clients;

public class ClientRequest {
    private Client client;
    private String request;

    public ClientRequest(Client client, String request) {
        this.client = client;
        this.request = request;
    }

    public Client getClient() {
        return client;
    }

    public String getRequest() {
        return request;
    }
}
