package main.clients;

import main.cryptography.ServerCryptography;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * Super class for Client_Android (user) and Client_HomeServer (hub)
 */

public class Client {

    private final int homeServerID;
    private final Socket socket;
    private final DataInputStream input;
    private final DataOutputStream output;
    public ServerCryptography crypto;


    public Client(int systemID, Socket socket, DataInputStream input, DataOutputStream output) {
        this.homeServerID = systemID;
        this.socket = socket;
        this.input = input;
        this.output = output;
        this.crypto = new ServerCryptography();
    }

    public DataInputStream getInput() {
        return input;
    }

    public DataOutputStream getOutput() {
        return output;
    }

    public Socket getSocket() {
        return socket;
    }

    public int getHomeServerID() {
        return homeServerID;
    }

    public void writeToClient(byte[] message) throws IOException {
        output.writeInt(message.length);
        output.flush();
        output.write(message);
        output.flush();
    }

    public byte[] readFromClient() throws IOException {
        int length = input.readInt(); // Read message length
        if (length > 1 && length < 1000) {
            byte[] message = new byte[length];
            input.readFully(message, 0, message.length); // Read message
            return message;
        }
        else {
            throw new IOException("Suspicious message length");
        }
    }

    // Below: Symmetric encryption. Symmetric keys (AES and MAC) must first have been distributed

    public void writeEncryptedToClient(String message)  {
        try {
            byte[] encryptedMsg = crypto.symmetricEncryption(message);
            writeToClient(encryptedMsg);
        } catch (Exception e) {
            System.out.println("Unable to write encrypted msg to client");
            e.printStackTrace();
        }
    }

    public String readDecryptedFromClient() throws Exception {
        byte [] encryptedMsg = readFromClient();
        return crypto.symmetricDecryption(encryptedMsg);
    }

}
