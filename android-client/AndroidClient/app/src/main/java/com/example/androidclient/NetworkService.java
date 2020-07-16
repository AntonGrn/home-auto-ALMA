package com.example.androidclient;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;
import com.example.androidclient.utilities.ClientCryptography;
import com.example.androidclient.utilities.Updatable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class NetworkService extends Service {
    //Log for debugging
    private static final String TAG = "NetworkActivity";

    // Binder object to bind client(s) to the service
    private IBinder binder = new MyBinder();

    // Public server communication
    private volatile Socket socket;
    private Thread inputThread;
    public volatile boolean inputThreadRunning; // Variable to be checked inside thread loop.
    public volatile boolean connectedToServer;
    public BlockingQueue<String> requestsToServer;

    // Cryptography
    private volatile ClientCryptography crypto;
    private final Object lockObjectCrypto = new Object();

    // Gets called when the service is first started
    @Override
    public void onCreate() {
        super.onCreate();
        socket = null;
        inputThread = null;
        inputThreadRunning = false;
        connectedToServer = false;
        requestsToServer = new ArrayBlockingQueue<>(10);
        crypto = new ClientCryptography();
    }

    // binder: Used by the client (e.g. Activity) as reference to the service.
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    // Custom inner class that extends Binder
    // Binder: For retrieving a service instance, used by the client to communicate with the service
    public class MyBinder extends Binder {
        NetworkService getService() {
            //Return an instance of the service. Like a Singleton.
            return NetworkService.this;
        }
    }

    // onTaskRemoved will be called when the application is removed from the recently used applications list
    // -> stopSelf = a hard stop (used as precaution).
    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        // + stop threads running
        stopSelf();
    }

    // ============= CUSTOM UTILITY THREADS AND METHODS ==========================
    // inputThread will launch outputThread
    // inputThread will terminate outputThread when itself is terminated.

    public volatile boolean connectionException = false;

    public void connectToPublicServer(Handler handler, Updatable updatable, String ip, int port) {
        if (inputThread == null) {
            inputThread = new Thread(new ClientInputThread(handler, updatable, ip, port));
            inputThread.start();
        }
    }

    public void stopConnectionToPublicServer() {
        if (inputThreadRunning) {
            // Terminate inputThread
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        inputThread = null;
        inputThreadRunning = false;
        requestsToServer.clear();

        Log.d(TAG, "Thread stop requested");
    }

    // ====================== INPUT THREAD =======================================
    // - Invoked and managed by UI thread
    // - Initiates Public Server communication (Socket)
    // - Launches and manages OutputThread
    // - Listens for requests from public server, and notifies UI thread

    // inputThread is terminated by socket.close() -> IOException
    // outputThread is terminated by thread.interrupt() -> InterruptedException

    private class ClientInputThread implements Runnable {
        private String ip;
        private int port;
        // To reach method update() of UI thread (main activity)
        private Updatable updatableInstance;
        // Get a reference to UI thread's message queue
        private Handler handler;
        // Connection to public server
        private DataInputStream input;
        private DataOutputStream output;
        private Thread outputThread;
        boolean outputThreadRunning;

        ClientInputThread(Handler handler, Updatable updatableInstance, String ip, int port) {
            this.ip = ip;
            this.port = port;
            this.handler = handler;
            this.updatableInstance = updatableInstance;
            socket = null;
            input = null;
            output = null;
            outputThread = null;
            outputThreadRunning = false;
        }

        @Override
        public void run() {
            try {
                inputThreadRunning = true;

                Log.d(TAG, "Input thread started " + Thread.currentThread().getName());
                // Try to establish the connection with public server (IOException if not possible)
                socket = new Socket(ip, port);

                connectedToServer = true;

                // Obtaining input and output streams
                input = new DataInputStream(socket.getInputStream());
                output = new DataOutputStream(socket.getOutputStream());

                synchronized (lockObjectCrypto) {
                    // Receive server's public key (RSA)
                    crypto.setServersPublicKey(readFromServer());
                    // Generate secret keys (AES key + MAC key)
                    crypto.generateSymmetricKeys();
                }

                // Launch custom output thread
                outputThread = new Thread(new ClientOutputThread(output));
                outputThread.start();
                outputThreadRunning = true;

                // Start listening for input from public server
                while (true) {
                    // Read input from public server
                    byte [] encryptedServerRequest = readFromServer();
                    synchronized (lockObjectCrypto) {
                        // Decrypt the data
                        String decryptedServerRequest = crypto.symmetricDecryption(encryptedServerRequest);
                        // Update UI thread
                        updateUIThread(decryptedServerRequest);
                    }
                }
            } catch (IOException e) { // Socket exceptions
                connectionException = true;
            } catch (Exception e) { // Cryptography exceptions
                connectionException = true;
                Log.d(TAG, "Cryptography exception: " +  e.getMessage());
                // Ignore. Just close.
            } finally {
                if (outputThreadRunning) {
                    outputThread.interrupt();
                    outputThreadRunning = false;
                }
                requestsToServer.clear();
                closeResources();
                inputThreadRunning = false;
                connectedToServer = false;
                inputThread = null;
                // Notify UI thread: Connection lost/terminated/unable to establish
                updateUIThread("20");

                Log.d(TAG, "Input thread closed " + Thread.currentThread().getName());
            }
        }

        private byte[] readFromServer() throws IOException {
            int length = input.readInt(); // Read message length
            if (length > 1 && length < 1000) {
                byte[] data = new byte[length];
                input.readFully(data, 0, data.length); // Read message
                return data;
            } else {
                throw new IOException("Suspicious input length");
            }
        }

        private void updateUIThread(final String request) {
            // Update UI thread
            handler.post(new Runnable() {
                @Override
                public void run() {
                    updatableInstance.update(request);
                }
            });
        }

        private void closeResources() {
            try {
                if (input != null) {
                    input.close();
                }
                if (output != null) {
                    output.close();
                }
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException e) {
                //Handle
            }
        }
    }

    // ====================== OUTPUT THREAD =======================================
    // Invoked and managed by inputThread

    private class ClientOutputThread implements Runnable {
        DataOutputStream output;

        ClientOutputThread(DataOutputStream output) {
            this.output = output;
        }

        @Override
        public void run() {

            Log.d(TAG, "Output thread started " + Thread.currentThread().getName());

            try {
                synchronized (lockObjectCrypto) {
                    // Distribute symmetric keys + send login request data
                    String loginRequest = requestsToServer.take();
                    writeToServer(crypto.createInitialMessage(loginRequest));
                }

                while (true) {
                    String clientRequest = requestsToServer.take();
                    synchronized (lockObjectCrypto) {
                        // Encrypt and send the data
                        writeToServer(crypto.symmetricEncryption(clientRequest));
                    }
                }
            } catch (Exception e) {
                // Ignore
            } finally {
                closeResources();
            }
        }

        private void writeToServer(byte[] data) throws IOException {
            output.writeInt(data.length); // Send message length
            output.flush();
            output.write(data); // Send message
            output.flush();
        }

        private void closeResources() {
            try {
                if (output != null) {
                    output.close();
                    Log.d(TAG, "Output thread closed " + Thread.currentThread().getName());
                }
            } catch (IOException e) {
                //Handle ??
            }
        }
    }


}
