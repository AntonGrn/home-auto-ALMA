package main.clients;

import main.Server;

import java.io.IOException;

public class ClientThread extends Thread {

    /**
     * {@link #login()}
     * Client may be either Client_Android or Client_HomeServer
     *
     * Before gaining access to the the input loop (and hence; the server features),
     * the client thread assures that
     * - Client obeys to cryptography scheme
     * - Client provides symmetric keys (AES key & MAC key for this session)
     * - Client provides valid login data (and obeys to communication protocol when doing so)
     * If not; thread cancels and removes client from clientList
     * Connections are valid if the client presents either:
     * - Valid username and password (for Android client login)
     * - Valid username and key (for Android client reconnection)
     * - Valid system id and password (for systems a.k.a. hubs)
     */

    private Client client;

    // Variables used for database log purposes (logging client traffic)
    private String log_clientName;
    private String log_clientType;
    private String log_clientIP;

    public ClientThread(Client client) {
        this.client = client;
        log_clientName = "-";
        log_clientType = "-";
        log_clientIP = getClientIP();
    }

    @Override
    public void run() {
        try {
            if (login()) {
                String messageFromClient;
                while (!Server.getInstance().terminateServer) {
                    // Read from client
                    messageFromClient = client.readDecryptedFromClient();
                    ClientRequest clientRequest = new ClientRequest(client, messageFromClient);

                    if (client instanceof Client_HomeServer && messageFromClient.equals("21")) {
                        throw new IOException("Home Server log out");
                        // Note: Android log out is handled in Server class, because it needs to destroy Android sessionKey
                    }
                    //Add to Server request list
                    Server.getInstance().clientRequests.put(clientRequest);
                }
            }
        } catch (Exception e) {
            // Ignore. Upon client disconnect or failed decryption.
        } finally {
            closeResources();
        }
    }

    // Establish cryptography keys and verify login request
    private boolean login() {
        boolean successfulCryptography = false;
        try {
            // Generate asymmetric keypair (private & public using RSA)
            client.crypto.generateAsymmetricKeyPair();
            // Send public key to client
            byte[] publicKey = client.crypto.getPublicKeyAsByteArray();
            client.writeToClient(publicKey);

            //Receive symmetric cryptography variables (AES-key & MAC-key) + login data
            byte[] inputFromClient = client.readFromClient();
            String loginRequest = client.crypto.processInitialClientInput(inputFromClient);

            successfulCryptography = true;

            // Verify login data + specialize client
            ClientRequest connectionRequest = new ClientRequest(client, loginRequest);
            client = Server.getInstance().establishConnection(connectionRequest); // Will assign the complete client information, incl specialization into Client_Android or Client_HomeServer

            // Log purposes
            if (client instanceof Client_Android) {
                log_clientName = ((Client_Android) client).getName().concat(((Client_Android) client).isAdmin() ? " [Admin]" : "");
                log_clientType = "Android";
            } else {
                log_clientName = String.valueOf(client.getHomeServerID());
                log_clientType = "Home Server";
                System.out.println("Home Server connected: " + client.getHomeServerID());
            }
            Server.getInstance().logConnectionLoginResult(log_clientName, log_clientType, log_clientIP);
            return true;
        } catch (Exception e) {
            if (successfulCryptography) {
                log_clientName = e.getMessage(); // "Invalid connection format" or "Unsuccessful login attempt"
            } else {
                log_clientName = "Invalid cryptography";
            }
            Server.getInstance().logConnectionLoginResult(log_clientName, log_clientType, log_clientIP);
            return false;
        }
    }

    private void closeResources() {
        try {
            Server.getInstance().logConnectionClose(log_clientName, log_clientIP);
            //Remove client from activeClients.
            if (client instanceof Client_Android) {
                Server.getInstance().removeAndroidClient(((Client_Android) client).getConnectionID());
            } else if (client instanceof Client_HomeServer) {
                Server.getInstance().removeHomeServerClient((Client_HomeServer) client);
                System.out.println("ClientThread closed for HomeServer: " + client.getHomeServerID());
            }
            //Close resources
            client.getSocket().close();
            client.getInput().close();
            client.getOutput().close();

        } catch (IOException e) {
            System.out.println("IOException on closing clientThread: " + Thread.currentThread().getName());
        }
    }

    // Used in log purposes
    private String getClientIP() {
        return client.getSocket().getInetAddress().toString().substring(1); // IP-format "/X.X.X.X" to "X.X.X.X"
    }
}
