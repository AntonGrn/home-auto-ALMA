package com.example.androidclient;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

public class ClientCryptography {

    /**
     * GOAL:     Use symmetric cryptography for the client-server communication.
     * PROBLEM:  Distribute symmetric (secret) AES key in secure way.
     * SOLUTION: Distribute symmetric keys using asymmetric cryptography.
     * *
     * APPROACH:
     * 1. Server sends its public key (asymmetric) to client.
     * 2. Client generates AES key, IV, MAC-key (for symmetric cryptography).
     * 3. Client encrypts AES key, IV, MAC-key with server's public key (asymmetric encryption).
     * 4. Client encrypts login data (payload) with AES-key, IV (symmetric encryption).
     * 5. Client generates MAC of the payload.
     * 5. Client sends encrypted: AES-key, IV, MAC-key, payload, MAC to server.
     * 6. Server decrypts AES-key, IV, MAC-key with Server's private key (asymmetric decryption).
     * 7. Server assigns the symmetric key variables (AES-key, IV, MAC-key).
     * 8. Server decrypts the payload using the symmetric key variables (symmetric decryption).
     * 9. Server verifies MAC.
     * 10.If verification is successful; the continuous communication between client and server
     * within the session will use symmetric cryptography (symmetric keys has been distributed).
     * *
     * *
     * FIRST THING SERVER WILL RECEIVE FROM CLIENT (after having sent client its public key):
     * *____________________________________________________________________
     * |                         |                                          |
     * |   Shared secrets for    |       Payload:                           |
     * | Symmetric cryptography  |       User authentication                |
     * |_________________________|__________________________________________|
     * |            |            |            |            |                |
     * |  AES-key   |  MAC-key   |    IV      |    MAC     |    Message     |
     * |  128 bit   |  128 bit   |  128 bit   |  128 bit   | Variable size  |
     * |____________|____________|____________|____________|________________|
     * |                         |            |                             |
     * |     Encrypted:          | Plaintext  |        Encrypted:           |
     * |    Asymmetric (RSA)     |            |      Symmetric (AES)        |
     * |_________________________|____________|_____________________________|
     * *
     * *
     * ENCRYPTED MESSAGES AFTER SYMMETRIC KEYS HAS BEEN DISTRIBUTED:
     * (For CBC: IV can securely be sent in plaintext)
     * *____________________________________________
     * |            |            |                  |
     * |    IV      |    MAC     |     Message      |
     * |  128 bit   |  128 bit   |  Variable size   |
     * |____________|____________|__________________|
     * |            |                               |
     * | Plaintext  |  Encrypted: Symmetric (AES)   |
     * |____________|_______________________________|
     * *
     * *
     * NOTE: The data between client-server is sent as byte array
     * *
     * ADVANTAGES OF HAVING EACH CLIENT THREAD GENERATE UNIQUE KEY PAIRS:
     * - Prevents replay attacks (since each new client TCP-connection receives unique keys).
     * *
     * ADVANTAGES OF DISTRIBUTING SYMMETRIC KEYS USING ASYMMETRIC CRYPTOGRAPHY
     * - If the symmetric keys would instead be calculated from the client's password
     * (at both client and server end) additional information would need to be given
     * to inform the server of which client's password it should generate symmetric key from.
     * = Unique client identifier would need to be passed for systems with more than one client.
     * - Possibility to use signatures and certificate for sender verification.
     */

    // ASYMMETRIC KEYS
    private PublicKey serversPublicKey;
    // SYMMETRIC KEYS (shared secret)
    private SecretKey AES_key;   // Key to encrypt/decrypt
    private SecretKey  MAC_key;  // Key to create MAC

    public void setServersPublicKey(byte[] receivedPublicKey) throws Exception {
        try {
            serversPublicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(receivedPublicKey));
        } catch (Exception e) {
            throw new Exception("Failure to set server's public key");
        }
    }

    public void generateSymmetricKeys() throws Exception{
        // AES key:
        KeyGenerator keyGenAES = KeyGenerator.getInstance("AES");
        keyGenAES.init(128, new SecureRandom()); // increase?
        AES_key = keyGenAES.generateKey();
        // MAC key:
        KeyGenerator keyGenMAC = KeyGenerator.getInstance("HMACMD5"); // AES?
        keyGenMAC.init(128, new SecureRandom()); // increase?
        MAC_key = keyGenMAC.generateKey();
    }

    public byte[] createInitialMessage(String loginData) throws Exception {
        byte[] enc_AES_key = asymmetricEncryptionRSA(AES_key.getEncoded());
        byte[] enc_MAC_key = asymmetricEncryptionRSA(MAC_key.getEncoded());
        byte[] enc_msg_AES = symmetricEncryption(loginData);

        byte[] secretKeys = concatByteArrays(enc_AES_key, enc_MAC_key);

        // AES key (encrypted:RSA), MAC key (encrypted:RSA), IV (plain text), MAC (encrypted: AES), message (encrypted: AES)
        return concatByteArrays(secretKeys, enc_msg_AES);
    }

    private byte[] asymmetricEncryptionRSA(byte[] plainText) throws Exception {
        // Create a Cipher instance for cryptography operation. State the algorithm to be used.
        Cipher decryptRSA = Cipher.getInstance("RSA/ECB/OAEPWithSHA1AndMGF1Padding");
        // Initialize the Cipher with: operation mode and key.
        decryptRSA.init(Cipher.ENCRYPT_MODE, serversPublicKey);
        // Return the encrypted data.
        return decryptRSA.doFinal(plainText); // byte array
    }

    // ================= Symmetric cryptography: Same for both communicating parties ======================

    public String symmetricDecryption(byte[] encryptedData) throws Exception {
        if (encryptedData.length < 40 || encryptedData.length > 1000) {
            throw new Exception("Client input of suspicious length: " + encryptedData.length);
        }
        // Split IV (plaintext) from encrypted MAC & message (ciphertext)
        byte[] IV = Arrays.copyOfRange(encryptedData, 0, 16); // 16 bytes = 128 bits ;
        byte[] cipherText = Arrays.copyOfRange(encryptedData, 16, encryptedData.length); // MAC + message

        // Create an Initial Vector instance from byte array.
        IvParameterSpec ivSpec = new IvParameterSpec(IV);
        // Create a Cipher instance and pass the algorithm to be used. CBC because we use IV
        Cipher decryptAES = Cipher.getInstance("AES/CBC/PKCS5Padding");
        // Initialize the Cipher with mode of operation, key and IV.
        decryptAES.init(Cipher.DECRYPT_MODE, AES_key, ivSpec);
        // Decrypt into byte array
        byte[] decryptedData = decryptAES.doFinal(cipherText);

        // Split decrypted MAC from decrypted message
        byte[] referenceMAC = Arrays.copyOfRange(decryptedData,0, 16); // 128 bit key
        byte[] decryptedMessage = Arrays.copyOfRange(decryptedData, 16, decryptedData.length);
        // Verify message authentication
        if (!verifyMAC(referenceMAC, decryptedMessage)) {
            throw new Exception("MAC did not match");
        }
        return new String(decryptedMessage); //plainText
    }

    public byte[] symmetricEncryption(String message) throws Exception { // returns IV + encrypted MAC + encrypted message (as byte array)
        byte[] messageByteArray = message.getBytes();
        byte[] MAC = generateMAC(messageByteArray);
        byte[] IV = generateRandomIV();

        // Concatenate MAC and message (in plain text)
        byte[] bufferToEncrypt = concatByteArrays(MAC, messageByteArray);
        // Encrypt MAC + message
        IvParameterSpec ivSpec = new IvParameterSpec(IV);
        Cipher encryptAES = Cipher.getInstance("AES/CBC/PKCS5Padding");
        encryptAES.init(Cipher.ENCRYPT_MODE, AES_key, ivSpec);
        byte[] cipherText = encryptAES.doFinal(bufferToEncrypt);
        // Append IV (in plaintext) at beginning of cipher text
        return concatByteArrays(IV, cipherText);
    }

    private byte[] concatByteArrays(byte[] firstArray, byte[] secondArray) {
        byte[] buffer = new byte[firstArray.length + secondArray.length];
        System.arraycopy(firstArray, 0, buffer, 0, firstArray.length);
        System.arraycopy(secondArray, 0, buffer, firstArray.length, secondArray.length);
        return buffer;
    }

    private byte[] generateRandomIV() {
        // Initial Vector (preventing equal plaintext blocks mapping to equal cipher text blocks. A "scrambler")
        SecureRandom randomSecureRandom = new SecureRandom();
        byte[] IV = new byte[16]; // block size ??
        randomSecureRandom.nextBytes(IV);
        return IV;
    }

    private boolean verifyMAC(byte[] referenceMAC, byte[] decryptedData)  {
        byte[] computedMAC = generateMAC(decryptedData);
        return (Arrays.equals(referenceMAC, computedMAC));
    }

    private byte[] generateMAC(byte[] message) {
        try {
            Mac mac = Mac.getInstance("HMACMD5");
            mac.init(MAC_key);
            return mac.doFinal(message); // MAC as byte array
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            e.printStackTrace();
        }
        return null;
    }





}
