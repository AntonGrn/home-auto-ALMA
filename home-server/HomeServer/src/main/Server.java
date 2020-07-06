package main;

import main.JSON.JSON_reader;
import main.cryptography.ClientCryptography;
import main.gadgets.Gadget;
import main.gadgets.automations.AutomationHandler;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Intended to be run as a Linux service on a Raspberry Pi
 **
 * Note: If you don't want to update the JRE of Raspberry Pi
 * (which normally support up to Java SE 8 = 52 ):
 **
 * Use prior compiling Java language level:
 * File -> Project Structure -> Project:
 * Project Language Level: 8 -> Apply
 */

public class Server {
    private String homeServerAlias;
    private volatile boolean terminateServer;
    private volatile boolean debugMode;
    private Socket publicServerSocket;
    private DataInputStream publicServerInput;
    private DataOutputStream publicServerOutput;
    private ClientCryptography crypto;
    private List<Gadget> gadgetList;
    private volatile AutomationHandler automations;
    private final JSON_reader JSON_reader;
    public BlockingQueue<String> requestsToHomeServer;

    // Lock objects
    private final Object lock_gadgetList;
    private final Object lock_pollGadgets;
    private final Object lock_processRequests;
    private final Object lock_publicServer;
    private final Object lock_closeServer;
    private final Object lock_debugLogs;

    // Worker threads
    private Thread pollGadgetsThread;
    private Thread processRequestsThread;

    // Make Singleton
    private static Server instance = null;

    public static Server getInstance() {
        if (instance == null) {
            instance = new Server();
        }
        return instance;
    }

    private Server() {
        homeServerAlias = null;
        publicServerSocket = null;
        publicServerInput = null;
        publicServerOutput = null;
        crypto = new ClientCryptography();
        gadgetList = Collections.synchronizedList(new ArrayList<>());
        automations = null;
        JSON_reader = new JSON_reader();
        requestsToHomeServer = new ArrayBlockingQueue<>(10);
        terminateServer = false;
        debugMode = false;

        lock_gadgetList = new Object();
        lock_pollGadgets = new Object();
        lock_processRequests = new Object();
        lock_publicServer = new Object();
        lock_closeServer = new Object();
        lock_debugLogs = new Object();

        pollGadgetsThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    pollGadgets();
                } catch (Exception e) {
                    System.out.println("Exception in pollGadgetThread");
                    closeHomeServer();
                }
            }
        });

        processRequestsThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    processRequests();
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                    closeHomeServer();
                }
            }
        });
    }

    public void launchHomeServer() {
        System.out.println("Home server running...");
        try {
            // Read in config data from JSON file (config.json):
            String[] configData = JSON_reader.loadConfigData();
            // Process config data from JSON file (config.json)
            int hub_ID = Integer.parseInt(configData[0]);
            homeServerAlias = configData[1];
            String hub_password = configData[2];
            debugMode = configData[3].equals("true");
            String public_server_IP = configData[4];
            int public_server_port = Integer.parseInt(configData[5]);

            debugLog("Read from 'config.json'",
                    String.valueOf(hub_ID), homeServerAlias, hub_password, String.valueOf(debugMode),
                    public_server_IP, String.valueOf(public_server_port));

            populateGadgetList();
            automations = new AutomationHandler(JSON_reader.loadAutomations());
            connectToPublicServer(hub_ID, hub_password, public_server_IP, public_server_port);
            //Deploy worker threads
            pollGadgetsThread.start();
            processRequestsThread.start();
            //infinite loop listening for inputs from the public server
            listenForPublicServerInput();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        } finally {
            closeHomeServer();
            System.out.println("Thread terminated: main thread");
        }
    }

    // =============================== PUBLIC SERVER CONNECTION =============================================

    private void connectToPublicServer(int hub_ID, String hub_password, String public_server_IP, int public_server_port) throws Exception {

        synchronized (lock_publicServer) {
            try {
                publicServerSocket = new Socket(public_server_IP, public_server_port);

                // Obtaining input and output streams
                publicServerInput = new DataInputStream(publicServerSocket.getInputStream());
                publicServerOutput = new DataOutputStream(publicServerSocket.getOutputStream());

                // Receive server's public key (RSA)
                crypto.setServersPublicKey(readFromServer());
                // Generate secret keys (AES key + MAC key)
                crypto.generateSymmetricKeys();

                // Distribute secret (symmetric) keys + send login request data
                String loginRequest = String.format("%s%s%s%s%s%s", "6:", hub_ID, ":", hub_password, ":", homeServerAlias);
                debugLog("Login request to public server", loginRequest);
                writeToServer(crypto.createInitialMessage(loginRequest));

                // Symmetric cryptography may now begin.
                // Read login result from public server
                String loginResult = readDecryptedFromServer();
                debugLog("Login response from public server", loginResult);

                // Process login request from public server
                String[] results = loginResult.split(":");

                if (results[0].equals("7")) {
                    if (results[1].equals("ok")) {
                        System.out.println("Connected to public server as: " + homeServerAlias);
                    } else {
                        throw new Exception(results[2]);
                    }
                } else {
                    throw new Exception("Invalid login. Connection is good");
                }
            } catch (IOException e) {
                throw new Exception("Unable to setup connection to public server");
            } catch (Exception e) {
                throw new Exception("Invalid cryptography");
            }
        }
    }

    // ====================== PUBLIC SERVER COMMUNICATION ==================================================

    // Indefinite loop
    private void listenForPublicServerInput() throws Exception {
        String request = "";
        try {
            while (!terminateServer) {
                request = readDecryptedFromServer();
                requestsToHomeServer.put(request);
            }
        } catch (Exception e) {
            throw new Exception("Unable to read request from server: " + request);
        }
    }

    private String readDecryptedFromServer() throws Exception {
        try {
            return crypto.symmetricDecryption(readFromServer());
        } catch (IOException e) { //Socket issue
            throw new Exception("Socket exception on reading from server\n" + e.getMessage());
        } catch (Exception e) { // Cryptography issue
            throw new Exception("Cryptography exception on reading from server\n" + e.getMessage());

        }
    }

    private byte[] readFromServer() throws IOException {
        int length = publicServerInput.readInt(); // Read message length
        if (length > 1 && length < 1000) {
            byte[] data = new byte[length];
            publicServerInput.readFully(data, 0, data.length); // Read message
            return data;
        } else {
            throw new IOException("Suspicious input length");
        }
    }

    private void writeEncryptedToServer(String message) {

        debugLog("Message to public server", message);

        try {
            writeToServer(crypto.symmetricEncryption(message));
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private void writeToServer(byte[] message) {
        try {
            publicServerOutput.writeInt(message.length); // Send message length
            publicServerOutput.flush();
            publicServerOutput.write(message); // Send message
            publicServerOutput.flush();
        } catch (IOException e) {
            System.out.println("Unable to send message to server: " + new String(message));
        }

    }

    // ================================== MANAGE GADGET LIST ================================================

    private void populateGadgetList() throws Exception{
        synchronized (lock_gadgetList) {
            try {
                // Read in gadgets from JSON file:
                gadgetList = JSON_reader.loadAllGadgets();
            } catch (Exception e) {
                throw new Exception("Error on reading gadgets from JSON file");
            }
        }
    }

    // Executed by worker thread: pollGadgetsThread
    private void pollGadgets() throws Exception {
        synchronized (lock_pollGadgets) {
            int nbrOfGadgets = 0;
            synchronized (lock_gadgetList) {
                nbrOfGadgets = gadgetList.size();
            }
            while (!terminateServer) {
                boolean updateClients = false;
                for (int i = 0; i < nbrOfGadgets; i++) {
                    synchronized (lock_gadgetList) {
                        Gadget gadget = gadgetList.get(i);
                        long currentMillis = System.currentTimeMillis();
                        // Verify if it's time to poll gadget:
                        if ((currentMillis - gadget.lastPollTime) > gadget.pollDelayMs) {
                            // Store pre-poll values:
                            int expectedState = gadget.getState();
                            boolean expectedPresence = gadget.isPresent;
                            // Poll the gadget:
                            gadget.poll();
                            // Upon successful poll: Set timestamp. Else: Try again at next iteration.
                            if(gadget.isPresent) {
                                gadget.lastPollTime = currentMillis;
                            }
                            debugLog(gadget.alias, gadget.isPresent, gadget.getState());
                            // Verify if gadget states changed by the poll() operation:
                            if (gadget.isPresent != expectedPresence || gadget.getState() != expectedState) {
                                updateClients = true;
                                if(gadget.getState() != expectedState) {
                                    // Check if gadget state change triggers any automations
                                    automations.newEvent(gadget.gadgetID, gadget.getState());
                                }
                            }
                        }
                    }
                    if (terminateServer) {
                        return;
                    }
                    Thread.sleep(50);
                }
                if (updateClients) {
                    sendAllGadgetsToPublicServer("-1");
                }
                // Minimum poll delay: 2 sec
                // Only place where pollGadgets() may throw exception (InterruptedException)
                Thread.sleep(2000);
                // Check if any automations should fire.
                automations.runTimeScan();
            }
        }
    }

    //========================= PROCESS REQUESTS FROM PUBLIC SERVER ==================================

    // Executed by worker thread: processRequestsThread
    private void processRequests() throws Exception {
        while (!terminateServer) {
            synchronized (lock_processRequests) {
                try {
                    String request = requestsToHomeServer.take();
                    String[] commands = request.split(":");

                    debugLog("Request to home server", request);

                    switch (commands[0]) {
                        case "9":
                            alterGadgetState(commands[1], commands[2], commands[3]);
                            break;
                        case "11":
                            sendAllGadgetsToPublicServer(commands[1]);
                            break;
                        default:
                            System.out.println("Invalid command sent from public server: " + request);
                            break;
                    }
                } catch (InterruptedException e) {
                    throw new Exception("Thread terminated: processRequestsThread");
                } catch (Exception e) {
                    // Ignore & carry on.
                }
            }
        }
    }

    // #9
    private void alterGadgetState(String androidID, String gadgetIDString, String newState) throws Exception {
        int gadgetID = Integer.parseInt(gadgetIDString);
        int requestedState = Integer.parseInt(newState);
        boolean gadgetFound = false;
        synchronized (lock_gadgetList) {
            for (Gadget gadget : gadgetList) {
                if (gadget.gadgetID == gadgetID) {
                    gadgetFound = true;
                    try {
                        gadget.alterState(requestedState);
                        sendAllGadgetsToPublicServer("-1");
                        // Check if gadget change triggers any automations.
                        automations.newEvent(gadget.gadgetID, gadget.getState());
                    } catch (Exception e) {
                        gadget.isPresent = false;
                        if(androidID.equals("-1")) {
                            // Request comes from home server automation instance
                            System.out.println("Automation unable to reach gadget " + gadget.alias);
                        } else {
                            // Request comes from single Android client
                            String exceptionMessage = "Unable to reach gadget " + gadget.alias;
                            System.out.println(exceptionMessage);
                            sendAllGadgetsToPublicServer(androidID);
                            writeEncryptedToServer(String.format("%s%s%s%s", "19:", exceptionMessage, ":", androidID));
                        }
                    }
                    break;
                }
            }
        }
        if (!gadgetFound) {
            String exceptionMessage = "Unable to find gadget " + gadgetID;
            System.out.println(exceptionMessage);
            writeEncryptedToServer(String.format("%s%s%s%s", "19:", exceptionMessage, ":", androidID));
        }
    }

    // #11
    private void sendAllGadgetsToPublicServer(String androidConnID) {
        // This method will be used in two cases: (1) new user requests all gadget, and (2) a change has been detected on a gadget upon gadget poll.
        // androidConnID == -1: Send to all clients of the home server, else send to individual androidConnID.
        String gadgetString = (androidConnID.equals("-1") ? "13:" : String.format("%s%s", "12:", androidConnID.concat(":")));
        synchronized (lock_gadgetList) {
            if (gadgetList.isEmpty()) {
                gadgetString = gadgetString.concat("null");
            } else {
                gadgetString = gadgetString.concat("notnull:");
                for (int i = 0; i < gadgetList.size(); i++) {
                    Gadget gadget = gadgetList.get(i);
                    if (gadget.isPresent) {
                        int gadgetID = gadget.gadgetID;
                        String gadgetName = gadget.alias;
                        String type = gadget.type.toString();
                        String currentState = String.valueOf(gadget.getState());

                        gadgetString = gadgetString.concat(gadgetID + ":" + gadgetName + ":" + type + ":" + currentState + ":");

                        if (i == (gadgetList.size() - 1)) {
                            gadgetString = gadgetString.concat("null");
                        } else {
                            gadgetString = gadgetString.concat("next:");
                        }
                    }
                }
            }
        }
        writeEncryptedToServer(gadgetString);
    }

    // ============================== CLOSE RESOURCES =================================================

    public void closeHomeServer() {
        synchronized (lock_closeServer) {
            if (!terminateServer) {
                terminateServer = true;
                closePublicServerConnection();
                closeThreads();
            }
        }
    }

    private void closePublicServerConnection() {
        // Send log out message to cloud. Cloud terminates System’s client thread.
        writeEncryptedToServer("21");

        if (publicServerInput != null) {
            try {
                publicServerInput.close();
            } catch (IOException e) {
                System.out.println("Unable to close publicServerInput");
            }
        }
        if (publicServerOutput != null) {
            try {
                publicServerOutput.close();
            } catch (IOException e) {
                System.out.println("Unable to close publicServerOutput");
            }
        }
        if (publicServerSocket != null) {
            try {
                publicServerSocket.close();
            } catch (IOException e) {
                System.out.println("Unable to close publicServerSocket");
            }
        }
        System.out.println("Public server connection closed");
    }

    private void closeThreads() {
        /*
        Note regarding threads:
        thread.interrupt() only sets the interrupted flag. This can only be used to close the thread if the
        thread implementation is listening for this flag for its operations.
        E.g: some blocking methods will not be forcibly interrupted by this, but rather requires pinpointed
        calls to the blocking section.
        In the same way: Setting a loop condition will not affect a blocking section until it wakes up and allows
        further method iteration.
         */

        if (pollGadgetsThread.isAlive() && !pollGadgetsThread.isInterrupted()) {
            // sleep() can be interrupted
            pollGadgetsThread.interrupt();
        }

        if (processRequestsThread.isAlive() && !processRequestsThread.isInterrupted()) {
            // queue.take() can be interrupted
            processRequestsThread.interrupt();
        }
    }

    // ================================== DEBUG LOGS =====================================================

    private void debugLog(String title, String... content) {
        synchronized (lock_debugLogs) {
            if(debugMode) {
                for (String log : content) {
                    // Note: Standard output stream writes to log file.
                    System.out.println(String.format("%-40s%s", title.concat(":"), (log.length() > 60 ? log.substring(0, 59)+" [...]" : log )));
                }
            }
        }
    }

    private void debugLog(String gadgetName, boolean presence, int state) {
        synchronized (lock_debugLogs) {
            if(debugMode) {
                String log = String.format("%-39s [%s][state:%s]",
                        "Polling gadget: ".concat(gadgetName), (presence ? "present" : "not present"), state);
                System.out.println(log);
            }
        }
    }

}
