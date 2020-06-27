package main;

import main.JSON.JSON_reader;
import main.clients.*;
import main.database.DB_Clients;
import main.database.DB_TrafficLogging;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

    public BlockingQueue<ClientRequest> clientRequests;

    private final List<Client_Android> connectedAndroidClients;
    private final List<Client_HomeServer> connectedHomeServers;

    private ServerSocket serverSocket;
    private int serverTcpPort;
    private int clientThreadPoolLimit;
    private volatile boolean debugMode;

    private Thread processRequestsThread;
    public volatile boolean terminateServer;

    private final Object lock_acceptClientConn;
    private final Object lock_processRequests;
    private final Object lock_DB_clients;
    private final Object lock_DB_trafficLogs;
    private final Object lock_debugLogs;
    private final Object lock_closeServer;

    private static Server instance = null;

    private Server() {
        clientRequests = new ArrayBlockingQueue<>(10);
        connectedAndroidClients = Collections.synchronizedList(new ArrayList<>());
        connectedHomeServers = Collections.synchronizedList(new ArrayList<>());
        lock_acceptClientConn = new Object();
        lock_processRequests = new Object();
        lock_DB_clients = new Object();
        lock_DB_trafficLogs = new Object();
        lock_debugLogs = new Object();
        lock_closeServer = new Object();
        serverSocket = null;
        terminateServer = false;
        debugMode = false;
        processRequestsThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    processClientRequests();
                } catch (InterruptedException e) {
                    closeServer();
                }
            }
        });
    }

    public static Server getInstance() {
        if (instance == null) {
            instance = new Server();
        }
        return instance;
    }

    public void launch() {
        try {
            System.out.println("ALMA server running...");
            // Read serverSocket specs from JSON file (config.json)
            int[] serverSpecs = JSON_reader.loadServerSpecs();
            serverTcpPort = serverSpecs[0];
            clientThreadPoolLimit = serverSpecs[1];
            debugMode = serverSpecs[2] == 1;
            debugLog("Read from 'config.json'", "server port :"+ String.valueOf(serverTcpPort),
                    "thread pool limit: " + String.valueOf(clientThreadPoolLimit),
                    "debug mode: " + String.valueOf(debugMode));
            // Launch iterative methods
            processRequestsThread.start();
            acceptClientConnections();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            closeServer();
        }
    }

    private void closeServer() {
        synchronized (lock_closeServer) {
            if (!terminateServer) {
                terminateServer = true;
                processRequestsThread.interrupt();
                clientRequests = null;
                try {
                    if(serverSocket != null) {
                        serverSocket.close();
                    }
                } catch (IOException e) {
                    System.out.println("Unable to close ServerSocket");
                }
                System.out.println("ALMA server terminated");
            }
        }
    }

    // ======================================== ESTABLISH CONNECTIONS =================================================

    private void acceptClientConnections() throws Exception {
        synchronized (lock_acceptClientConn) {

            // Thread pool to manage ClientThreads.
            ExecutorService executor = Executors.newFixedThreadPool(clientThreadPoolLimit);

            serverSocket = new ServerSocket(serverTcpPort);

            while (!terminateServer) {

                Socket clientConnection = null;
                Client client = null;

                try {
                    // Receive client connection requests
                    clientConnection = serverSocket.accept();

                    // obtaining input and output streams
                    DataInputStream input = new DataInputStream(clientConnection.getInputStream());
                    DataOutputStream output = new DataOutputStream(clientConnection.getOutputStream());

                    client = new Client(-1, clientConnection, input, output);

                    // Assigning new thread for client
                    executor.submit(new ClientThread(client));

                } catch (Exception e) {
                    if (clientConnection != null) {
                        clientConnection.close();
                    }
                    System.out.println("Exception in acceptClients");
                }
            }
            // Hard shutdown. System terminating.
            executor.shutdownNow();
        }
    }

    //Invoked by ClientThread to assure account is logged in to proceed
    public Client establishConnection(ClientRequest loginRequest) throws Exception {
        synchronized (lock_DB_clients) {
            Client client = loginRequest.getClient();

            debugLog("Client connection request", loginRequest.getRequest());

            String[] commands = loginRequest.getRequest().split(":");

            try {
                switch (commands[0]) {
                    // 1.  Android login (Username, password)
                    // 4.  Android reconnect (username, key)
                    // 6.  Home serverSocket connection (homeServerID, password)
                    case "1":
                        client = androidLoginWithPassword(client, commands[1], commands[2]);
                        break;
                    case "4":
                        client = androidLoginWithSessionKey(client, commands[1], commands[2]);
                        break;
                    case "6":
                        client = homeServerLogin(client, commands[1], commands[2], commands[3]);
                        break;
                    default:
                        break;
                }
                return client;
            } catch (IndexOutOfBoundsException | NumberFormatException e) {
                throw new Exception("Invalid connection format");
            }
        }
    }

    // #1
    private Client androidLoginWithPassword(Client client, String userName, String password) throws Exception {
        try {
            String newSessionKey = generateSessionKey(userName);
            //Try to log in with user name and password
            String[] result = DB_Clients.getInstance().androidLogin(userName, password, newSessionKey);

            int homeServerID = Integer.parseInt(result[0]);
            boolean admin = result[1].equals("1");

            // Specialize client
            client = new Client_Android(homeServerID, client, userName, admin);

            //Add client to list of active Android clients
            addAndroidClient((Client_Android) client);

            String clientOutput = String.format("%s%s%s%s%s%s%s%s",
                    "2:ok:null:", userName, ":", admin, ":", getHomeServerName(homeServerID), ":", newSessionKey);
            outputToAndroidClients(true, false, clientOutput, client.getHomeServerID(),
                    ((Client_Android) client).getConnectionID());

            // Simulate client requesting all gadgets from associated Home System
            String requestAllGadgets = String.format("%s%s", "11:", ((Client_Android) client).getConnectionID());
            ClientRequest gadgetRequest = new ClientRequest(client, requestAllGadgets);
            clientRequests.put(gadgetRequest);
        } catch (Exception e) {
            // Forwarded exception message from DB_Clients
            String exceptionMessage = e.getMessage();
            client.writeEncryptedToClient("2:no:".concat(exceptionMessage));
            throw new Exception("Unsuccessful login attempt");
        }
        return client;
    }

    private String getHomeServerName(int homeServerID) {
        synchronized (connectedHomeServers) {
            for (Client_HomeServer homeServer : connectedHomeServers) {
                if (homeServer.getHomeServerID() == homeServerID) {
                    return homeServer.getAlias();
                }
            }
            return "Hub not connected";
        }
    }

    private String generateSessionKey(String name) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");

        //Generate a string to hash: user name + current time
        String dataString = String.format("%s%s", name, (new SimpleDateFormat("HHmmss").format(new Date())));

        byte[] data = dataString.getBytes();
        String myChecksum = "";

        md.update(data);
        for (byte b : md.digest()) {                  // ... for each byte read
            myChecksum += String.format("%02X", b);   // ... add it as a hexadecimal number to the checksum
        }
        return myChecksum;
    }

    // #4
    private Client androidLoginWithSessionKey(Client client, String userName, String sessionKey) throws Exception {
        try {
            //DB_Clients: return a client if match on sessionKey AND username
            String[] result = DB_Clients.getInstance().androidReconnect(userName, sessionKey);

            int homeServerID = Integer.parseInt(result[0]);
            String admin = result[1];

            // Specialize client
            client = new Client_Android(homeServerID, client, userName, admin.equals("1"));

            //Add client to list of connected Android clients
            addAndroidClient((Client_Android) client);

            String clientOutput = "5:ok:null:";
            outputToAndroidClients(true, false, clientOutput, client.getHomeServerID(),
                    ((Client_Android) client).getConnectionID());

            // Simulate client requesting all gadgets from associated Home System
            String requestAllGadgets = String.format("%s%s", "11:", ((Client_Android) client).getConnectionID());
            ClientRequest gadgetRequest = new ClientRequest(client, requestAllGadgets);
            clientRequests.put(gadgetRequest);
        } catch (Exception e) {
            // Forwarded exception message from DB_Clients
            String exceptionMessage = e.getMessage();
            client.writeEncryptedToClient("5:no:".concat(exceptionMessage));
            throw new Exception("Unsuccessful login attempt");
        }
        return client;
    }

    // #6
    private Client homeServerLogin(Client client, String homeServerID_String, String password, String homeServerAlias) throws Exception {
        int systemID = Integer.parseInt(homeServerID_String);
        try {
            if (homeServerAlreadyLoggedIn(systemID)) {
                throw new Exception("Home server already logged in");
            }
            // Verify login (throws exception on failure)
            DB_Clients.getInstance().homeServerLogin(systemID, password);

            // Specialize client
            client = new Client_HomeServer(systemID, client, homeServerAlias);

            // Add client to list of connected home servers clients
            addHomeServerClient((Client_HomeServer) client);

            String clientOutput = "7:ok:null";
            outputToHomeServers(clientOutput, systemID);
        } catch (Exception e) {
            // Forwarded exception message from DB_Clients
            String exceptionMessage = e.getMessage();
            client.writeEncryptedToClient("7:no:".concat(exceptionMessage));
            throw new Exception("Unsuccessful system conn");
        }
        return client;
    }

    // Each home serverSocket should only have one instance (connected hub)
    private boolean homeServerAlreadyLoggedIn(int homeServerID) {
        synchronized (connectedHomeServers) {
            for (Client_HomeServer client : connectedHomeServers) {
                if (client.getHomeServerID() == homeServerID) {
                    return true;
                }
            }
            return false;
        }
    }

    // ======================================== MANAGE CONNECTED CLIENTS ===============================================

    //Invoked by establishConnection()
    private void addAndroidClient(Client_Android clientToAdd) {
        synchronized (connectedAndroidClients) {
            connectedAndroidClients.add(clientToAdd);
        }
    }

    //Invoked by establishConnection()
    private void addHomeServerClient(Client_HomeServer clientToAdd) {
        synchronized (connectedHomeServers) {
            connectedHomeServers.add(clientToAdd);
            outputToAndroidClients(false, false,
                    "18:Your home server has connected", clientToAdd.getHomeServerID(), -1);
        }
    }

    //Invoked by ClientThreads
    public void removeAndroidClient(int clientConnectionID) {
        synchronized (connectedAndroidClients) {
            for (int i = 0; i < connectedAndroidClients.size(); i++) {
                // Comparing connectionID instead of unique name allows a user to be active on multiple devices
                if (connectedAndroidClients.get(i).getConnectionID() == clientConnectionID) {
                    try {
                        // Only called from ClientThread, making it redundant to close resources from here too
                        connectedAndroidClients.get(i).getSocket().close();
                        connectedAndroidClients.get(i).getOutput().close();
                        connectedAndroidClients.get(i).getInput().close();
                    } catch (IOException e) {
                        System.out.println("Remove client error: Unable to close all resources for client " +
                                connectedAndroidClients.get(i).getName());
                    } finally {
                        connectedAndroidClients.remove(i);
                    }
                }
            }
        }
    }

    //Invoked by ClientThreads
    public void removeHomeServerClient(Client_HomeServer clientToRemove) {
        synchronized (connectedHomeServers) {
            for (int i = 0; i < connectedHomeServers.size(); i++) {
                if (connectedHomeServers.get(i).getHomeServerID() == clientToRemove.getHomeServerID()) {
                    int homeServerID = connectedHomeServers.get(i).getHomeServerID();
                    connectedHomeServers.remove(i);
                    outputToAndroidClients(false, false,
                            "18:Your home server has disconnected", homeServerID, -1);
                    break;
                }
            }
        }
    }

    // ======================================== PROCESS CLIENT REQUESTS ===============================================

    //Process requests from logged in clients (Client_Android & Client_HomeServer)
    private void processClientRequests() throws InterruptedException {
        ClientRequest clientRequest;
        while (!terminateServer) {
            //Waits here until there are any requests
            clientRequest = clientRequests.take();

            debugLog("Request from client", clientRequest.getRequest());

            synchronized (lock_processRequests) {
                try {
                    //Process the request according to ALMA communication protocol
                    String commands[] = clientRequest.getRequest().split(":");
                    Client client = clientRequest.getClient();

                    switch (commands[0]) {
                        case "3": // Android client log out: Destroy session key in DB_Clients
                            if (isAndroidClient(client)) {
                                androidLogout(((Client_Android) client).getName(), client.getHomeServerID(), ((Client_Android) client).getConnectionID());
                            }
                            break;
                        case "8": // Android client makes request to alter a gadget state.
                            if (isAndroidClient(client)) {
                                alterGadgetState(((Client_Android) client).getConnectionID(), commands[1], commands[2], client.getHomeServerID());
                            }
                            break;
                        case "11": // Android client requests all gadgets
                            outputToHomeServers(clientRequest.getRequest(), client.getHomeServerID());
                            break;
                        case "12": //Home server sends all gadget info, to FORWARD to individual Android client
                            if (isHomeServer(client)) {
                                forwardGadgetDataToAndroid(true, client.getHomeServerID(), commands[1], clientRequest.getRequest());
                            }
                            break;
                        case "13": // Home server sends all gadget info, to FORWARD to all Android clients of that home server
                            if (isHomeServer(client)) {
                                forwardGadgetDataToAndroid(false, client.getHomeServerID(), "-1", clientRequest.getRequest());
                            }
                            break;
                        case "19": // Forward exception message from home server to Android client
                            if (isHomeServer(client)) {
                                try {
                                    int androidConnectionID = Integer.parseInt(commands[2]);
                                    outputToAndroidClients(true, false, String.format("%s%s", "18:", commands[1]), client.getHomeServerID(), androidConnectionID);
                                } catch (NumberFormatException e) {
                                    System.out.println("Invalid command format from home server: " + client.getHomeServerID());
                                }
                            }
                            break;
                        default:
                            if (isAndroidClient(client)) {
                                outputToAndroidClients(true, false, "18:Invalid server request", client.getHomeServerID(), ((Client_Android) client).getConnectionID());
                            }
                    }
                } catch (Exception e) {
                    // Ignore
                }
            }
        }
    }

    // #3
    private void androidLogout(String androidName, int homeServerID, int androidConnectionID) {
        synchronized (lock_DB_clients) {
            try {
                DB_Clients.getInstance().destroySessionKey(androidName);
            } catch (Exception e) {
                outputToAndroidClients(true, false, e.getMessage(), homeServerID, androidConnectionID);
            }
        }
    }

    // #8
    // Forward a request to alter a gadget state from Android to home server. Append Android connectionID of triggering Android device
    private void alterGadgetState(int androidConnectionID, String gadgetID, String requestedGadgetState, int homeServerID) {
        // Append client connectionID to the message to the home server
        String connectionID = String.valueOf(androidConnectionID);
        String requestToHomeServer = String.format("%s%s%s%s%s%s", "9:", connectionID, ":", gadgetID, ":", requestedGadgetState);
        // Send request to specified home server
        outputToHomeServers(requestToHomeServer, homeServerID);
    }

    // #12&13
    private void forwardGadgetDataToAndroid(boolean onlyToIndividual, int homeServerID, String androidConnectionID, String message) {
        // Exclude all but gadget information from the request
        int excludeBeforeIndex;
        if (onlyToIndividual) {
            excludeBeforeIndex = message.indexOf(":", 3); // Excluding "11:connID:"
        } else {
            excludeBeforeIndex = message.indexOf(":"); // Excluding "12:"
        }
        message = message.substring(excludeBeforeIndex + 1);
        // Form a new request to Android client
        message = String.format("%s%s", "14:", message);

        try {
            int receiverConnectionID = Integer.parseInt(androidConnectionID);
            outputToAndroidClients(onlyToIndividual, false, message, homeServerID, receiverConnectionID);
        } catch (NumberFormatException e) {
            System.out.println("Invalid command format detected in forwardGadgetDataToAndroid");
        }
    }

    private boolean isAndroidClient(Client client) {
        return client instanceof Client_Android;
    }

    private boolean isHomeServer(Client client) {
        return client instanceof Client_HomeServer;
    }

    // ======================================== OUTPUT TO CLIENTS =====================================================


    //The final output operation
    private void outputToAndroidClients(boolean onlyToIndividual, boolean onlyToAdmins, String message, int homeServerID, int receiverConnectionID) {
        synchronized (connectedAndroidClients) {
            if (onlyToIndividual) {
                debugLog("Output to individual Android client", message);
                for (Client_Android client : connectedAndroidClients) {
                    if ((onlyToAdmins & client.isAdmin()) || !onlyToAdmins) {
                        // Comparing client connectionID rather than the unique client name,
                        // allows a user to be logged in on multiple devices and get correct serverSocket response.
                        if ((client.getConnectionID() == receiverConnectionID) && client.getHomeServerID() == homeServerID) {
                            client.writeEncryptedToClient(message);
                            break;
                        }
                    }
                }
            } else {
                debugLog("Output to all Android clients of home server ID " + homeServerID, message);
                for (Client_Android client : connectedAndroidClients) {
                    if (client.getHomeServerID() == homeServerID) {
                        if ((onlyToAdmins & client.isAdmin()) || !onlyToAdmins) {
                            client.writeEncryptedToClient(message);
                        }
                    }
                }
            }
        }
    }

    private void outputToHomeServers(String message, int homeServerID) {
        synchronized (connectedHomeServers) {
            boolean homeServerFound = false;
            debugLog("Output to home server " + homeServerID, message);
            for (Client_HomeServer system : connectedHomeServers) {
                if (system.getHomeServerID() == homeServerID) {
                    system.writeEncryptedToClient(message);
                    homeServerFound = true;
                }
            }
            if (!homeServerFound) {
                outputToAndroidClients(false, false, "18:Your home server is not connected", homeServerID, -1);
            }
        }
    }

    // ======================================== MANAGE DATABASE CLIENT LOGS ===========================================

    public void logConnectionLoginResult(String clientName, String clientType, String clientIP) {
        synchronized (lock_DB_trafficLogs) {
            try {
                DB_TrafficLogging.getInstance().clientConnectionLoginResult(clientName, clientType, clientIP);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    public void logConnectionClose(String clientName, String clientIP) {
        synchronized (lock_DB_trafficLogs) {
            try {
                DB_TrafficLogging.getInstance().clientConnectionClose(clientName, clientIP);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    // ================================== DEBUG LOGS ==================================================================

    private void debugLog(String title, String... content) {
        synchronized (lock_debugLogs) {
            if(debugMode) {
                for (String log : content) {
                    // Note: Standard output stream writes to log file.
                    System.out.println(String.format("%s%s%s", title, ": ", log));
                }
            }
        }
    }
}
