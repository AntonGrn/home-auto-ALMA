package main.gadgets;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;

public class Gadget_ALMA extends Gadget {
    private final String IP;
    private final int port;
    private final String requestSpec;
    private BufferedWriter output;
    private BufferedReader input;

    public Gadget_ALMA(int gadgetID, String name, GadgetType type, long pollDelaySeconds, String IP, int port, String requestSpec) {
        super(gadgetID, name, type, pollDelaySeconds);
        this.IP = IP;
        this.port = port;
        this.requestSpec = requestSpec;
    }

    // =================================== GADGET COMMUNICATION ================================================
    // Gadget communication according to ALMA application layer protocol.

    @Override
    public void poll() {
        String pollRequest = String.format("%s%s", "15:", requestSpec);
        try {
            String[] response = (sendCommand(pollRequest)).split(":");
            if (response[0].equals("16")) {
                setState(Integer.parseInt(response[1]));
                isPresent = true;
                return;
            }
        } catch (Exception e) {
            // Ignore
        }
        isPresent = false;
    }

    @Override
    public void alterState(int requestedState) throws Exception {
        if (type == GadgetType.CONTROL_ONOFF || type == GadgetType.CONTROL_VALUE) {
            String almaCommand = String.format("%s%s%s%s", "10:", requestedState, ":", requestSpec);
            String[] response = (sendCommand(almaCommand)).split(":");
            if (response[0].equals("16")) {
                setState(Integer.parseInt(response[1]));
            }
        }
    }

    @Override
    protected String sendCommand(String command) throws Exception {
        // Connect to gadget's TCP  server socket
        try (Socket gadgetSocket = new Socket()) {
            // Limits the time allowed to establish a connection
            gadgetSocket.connect(new InetSocketAddress(IP, port), 1500);
            // Force session timeout after specified interval after connection succeeds.
            gadgetSocket.setSoTimeout(3500);
            // Obtain output & input streams
            output = new BufferedWriter(new OutputStreamWriter(gadgetSocket.getOutputStream()));
            input = new BufferedReader(new InputStreamReader(gadgetSocket.getInputStream()));
            // Write encrypted request (ALMA protocol)
            output.write(encryptDecrypt(command).concat(String.format("%n")));
            output.flush();
            // Read encrypted response and decrypt it (to ALMA protocol format)
            return encryptDecrypt(input.readLine());
        } catch (IOException e) {
            return null;
        } finally {
            input.close();
            output.close();
        }
    }

    private String encryptDecrypt(String input) {
        char[] key = {'F', 'K', 'Q'};
        StringBuilder output = new StringBuilder();
        for(int i = 0 ; i < input.length() ; i++) {
            output.append((char)(input.charAt(i) ^ key[i % key.length]));
        }
        return output.toString();
    }
}
