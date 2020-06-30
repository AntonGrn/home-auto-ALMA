package main.gadgets;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;

public class Gadget_HS100 extends Gadget {
    private final String IP;
    private final int port;

    // JSON COMMANDS TO SMART PLUG
    private static final String COMMAND_SWITCH_ON = "{\"system\":{\"set_relay_state\":{\"state\":1}}}}";
    private static final String COMMAND_SWITCH_OFF = "{\"system\":{\"set_relay_state\":{\"state\":0}}}}";
    private static final String COMMAND_INFO = "{\"system\":{\"get_sysinfo\":null}}";

    public Gadget_HS100(int gadgetID, String name, long pollDelaySeconds, String IP, int port) {
        super(gadgetID, name, GadgetType.CONTROL_ONOFF, pollDelaySeconds);
        this.IP = IP;
        this.port = port;
    }

    @Override
    public void poll() {
        try {
            // Send request
            String jsonResponse = sendCommand(COMMAND_INFO);
            // Process response
            if (jsonResponse.length() > 0) {
                JSONObject jsonObject = (JSONObject) new JSONParser().parse(jsonResponse);
                JSONObject system = (JSONObject) jsonObject.get("system");
                JSONObject systemInfo = (JSONObject) system.get("get_sysinfo");
                int state = ((Long) systemInfo.get("relay_state")).intValue();
                // ON:1, OFF:0
                setState(state == 1 ? 1 : 0);
                isPresent = true;
            }
        } catch (Exception e) {
            isPresent = false;
        }
    }

    @Override
    public void alterState(int requestedState) throws Exception {
        if (type == GadgetType.CONTROL_ONOFF) {
            // Send request
            String jsonResponse = sendCommand(requestedState == 1 ? COMMAND_SWITCH_ON : COMMAND_SWITCH_OFF);
            // Process result
            if (jsonResponse.length() > 0) {
                JSONObject jsonObject = (JSONObject) new JSONParser().parse(jsonResponse);
                JSONObject system = (JSONObject) jsonObject.get("system");
                JSONObject setRelayState = (JSONObject) system.get("set_relay_state");
                int errorCode = ((Long) setRelayState.get("err_code")).intValue();
                if (errorCode == 0) {
                    setState(requestedState == 1 ? 1 : 0);
                }
            }
        }
    }

    @Override
    protected String sendCommand(String command) throws Exception {
        //Socket gadgetSocket;
        OutputStream output = null;
        InputStream input = null;
        String response = null;
        boolean isSuccess;

        // Connect to TCP server plug
        try (Socket gadgetSocket = new Socket()) {
            // Limits the time allowed to establish a connection
            gadgetSocket.connect(new InetSocketAddress(IP, port), 1500);
            // Force session timeout after specified interval after connection succeeds.
            gadgetSocket.setSoTimeout(3500);
            // Obtain output and input streams
            output = gadgetSocket.getOutputStream();
            input = gadgetSocket.getInputStream();
            // Write request (JSON String)
            output.write(encryptWithHeader(command));
            // Read response (JSON String)
            response = decrypt(input);
            isSuccess = true;
        } catch (IOException e) {
            isSuccess = false;
        } finally {
            if (output != null) {
                output.close();
            }
            if (input != null) {
                input.close();
            }
        }
        if (isSuccess) {
            return response;
        } else {
            throw new Exception("Failed to communicate with gadget");
        }
    }

    // ============================== TP-Link encryption ========================================================

    private String decrypt(InputStream inputStream) throws IOException {

        int in;
        int key = 0x2B;
        int nextKey;
        StringBuilder sb = new StringBuilder();
        while ((in = inputStream.read()) != -1) {

            nextKey = in;
            in = in ^ key;
            key = nextKey;
            sb.append((char) in);
        }
        return "{" + sb.toString().substring(5);
    }

    private int[] encrypt(String command) {

        int[] buffer = new int[command.length()];
        int key = 0xAB;
        for (int i = 0; i < command.length(); i++) {

            buffer[i] = command.charAt(i) ^ key;
            key = buffer[i];
        }
        return buffer;
    }

    private byte[] encryptWithHeader(String command) {

        int[] data = encrypt(command);
        byte[] bufferHeader = ByteBuffer.allocate(4).putInt(command.length()).array();
        ByteBuffer byteBuffer = ByteBuffer.allocate(bufferHeader.length + data.length).put(bufferHeader);
        for (int in : data) {

            byteBuffer.put((byte) in);
        }
        return byteBuffer.array();
    }
}
