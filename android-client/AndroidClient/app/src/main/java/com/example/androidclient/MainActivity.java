package com.example.androidclient;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import com.example.androidclient.fragments.AboutFragment;
import com.example.androidclient.fragments.HomeFragment;
import com.example.androidclient.fragments.LoginFragment;
import com.example.androidclient.fragments.LogoutFragment;
import com.example.androidclient.fragments.SettingsFragment;
import com.example.androidclient.fragments.SetupFragment;
import com.example.androidclient.utilities.Gadget;
import com.example.androidclient.utilities.GadgetType;
import com.example.androidclient.utilities.LoggedInUser;
import com.example.androidclient.utilities.ServerSpec;
import com.example.androidclient.utilities.Updatable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements Updatable {

    // Service. UI thread -> Service
    public NetworkService networkService;
    public volatile boolean isBound;

    // Handler: For Android thread communication. Service threads -> UI thread
    public Handler handler;

    // Avoid threads requesting illegal UI operations
    public volatile boolean appInFocus;

    // List of gadgets
    public ArrayList<Gadget> gadgetList;

    // Holds data of logged in user, or null if on one is logged in
    public LoggedInUser loggedInUser;

    // Holds ip and port of last established public server connection.
    public volatile ServerSpec server;
    public volatile boolean serverDefined;

    // Holds an updatable reference to the main activity (to be passed)
    private Updatable mainActivity = this;

    // Holds a reference to the fragment currently displayed
    public Updatable currentFragment;

    // To recognize a socket exception due to invalid sessionKey or invalid login
    public boolean loginAttemptWithSessionKey;

    // A demo mode for an example interface of the HomeFragment (DemoMode = Settings Options)
    public boolean demoMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false); // Removes default Toolbar title (Project name)

        networkService = null;
        isBound = false;
        handler = new Handler();
        loginAttemptWithSessionKey = false;
        loggedInUser = null;
        server = new ServerSpec("0", 0);
        serverDefined = false;
        gadgetList = new ArrayList<>();
        demoMode = false;
        currentFragment = null;
    }

    @Override
    protected void onResume() {
        super.onResume();
        getSupportActionBar().hide(); // Hide the toolbar until logged in

        // The Android service will be (re)started, or connected to if already running
        startService();

        appInFocus = true;

        // Each successful cloud connection will request the latest gadget states
        gadgetList.clear();

        // Default fragment SetupFragment is launched via onServiceConnected()
        //Create default fragment
        //fragmentTransaction("setup");
        readServerSpecFromCache();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isBound) {
            networkService.stopConnectionToPublicServer();
            unbindService(serviceConnection);
        }
        appInFocus = false;
    }

    // ====================== START AND BIND SERVICE =====================================
    // Server is alive for client(s) to bind & unbind to it,
    // until onTaskRemoved is called (in our case) from inside the Service.
    private void startService() {
        Intent intent = new Intent(this, NetworkService.class);
        startService(intent);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        // bindService: A call to the the service's onBind() method
        // BIND_AUTO_CREATE: The service will be created if it hasn't already been created
    }

    // WIll be called once the client-server connection (connection to the Service) has been established or disconnected
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // Will trigger when the client is successfully bound to the service, via bindService()
            // ComponentName: Represents what service the client is being bound to
            // IBinder: The link between the client and the service. What will be used for the connection.

            NetworkService.MyBinder binder = (NetworkService.MyBinder) service;

            //Obtain a reference to the service instance
            networkService = binder.getService();
            //Indicate that a connection has been successfully established
            isBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            // Will trigger when the client is unBound from the service = When connection ends
            isBound = false;
        }
    };

    // ======================== ESTABLISH SERVER CONNECTION =========================================

    // Establish connection to public server
    // Parameter loginRequest can be gathered from cache file (SetupFragment) or manual log in (LoginFragment)
    public void establishServerConnection(final String loginRequest) {
        // Tries connecting for 10 seconds
        new Thread(new Runnable() {
            volatile boolean connected = false;
            volatile int attempt = 0;
            volatile boolean requestAdded = false;

            public void run() {
                if (isBound) {
                    networkService.connectionException = false;
                }
                for (attempt = 0; attempt < 10; attempt++) {
                    Log.d("NetworkActivity", "Establish connection " + Thread.currentThread().getName());
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (!requestAdded && !networkService.connectionException) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // Client (UI thread) connected to Android bound service
                                if (isBound && appInFocus) {
                                    // Connect to server, or ignore if already connected
                                    networkService.connectToPublicServer(handler, mainActivity, server.IP, server.port);
                                    // Verify successfully launched connection thread
                                    connected = networkService.inputThreadRunning;
                                    if (connected) {
                                        Log.d("NetworkActivity", "CONNECTED");
                                        try {
                                            // Add log in request to be sent to server.
                                            networkService.requestsToServer.put(loginRequest);
                                            requestAdded = true;
                                        } catch (InterruptedException e) {
                                            writeToast("Unable to add login request");
                                        }
                                    }
                                }
                                if (attempt == 9 && !connected) {
                                }
                            }
                        });
                    }
                    if (connected || !appInFocus || networkService.connectionException) {
                        return;
                    }
                }
            }
        }).start();
    }

    // ======================== INTERFACE METHOD =========================================
    // Used by Service thread(s) to notify UI and pass update data
    // The data passed conform to the ALMA communication protocol and is decoded in update()

    @Override
    public void update(String command) {
        // Avoid UI-thread operations when client is not in focus on device
        if (appInFocus) {

            String[] commands = command.split(":");

            switch (commands[0]) {
                case "2":
                   loginResult(commands);
                    break;
                case "5":
                    reconnectionResult(commands);
                    break;
                case "14":
                    updateGadgetList(commands);
                    break;
                case "18": // Exception message from server (18) or from home server (19)
                case "19":
                    writeToast(commands[1]);
                    break;
                case "20": // Connection to server lost/terminated
                    lostServerConnection();
                    break;
                default:
                    writeToast("Unknown request from server: " + command);
                    break;
            }
        }
    }

    // #2
    private void loginResult(String[] commands) {
        if (commands[1].equals("ok")) {
            String userName = commands[3];
            boolean admin = commands[4].equals("1");
            String sysName = commands[5];
            String sessionKey = commands[6];
            loggedInUser = new LoggedInUser(userName, admin, sysName, sessionKey);

            fragmentTransaction("home");
            getSupportActionBar().show();

            writeUserToCache();
            writeServerSpecToCache();
        } else {
            writeToast(commands[2]);
        }
    }

    // #5
    private void reconnectionResult(String[] commands) {
        if (commands[1].equals("ok")) {
            fragmentTransaction("home");
            getSupportActionBar().show();
        } else {
            fragmentTransaction("login");
            writeToast("Invalid cache");
        }
    }

    // #14
    private void updateGadgetList(String[] commands) {
        gadgetList.clear();
        if (commands[1].equals("notnull")) {
            int count = 1;
            while (true) {
                int gadgetID = Integer.parseInt(commands[++count]);
                String gadgetName = commands[++count];
                String gadgetType = commands[++count];
                int gadgetState = Integer.parseInt(commands[++count]);

                gadgetList.add(new Gadget(gadgetID, gadgetName, gadgetState, GadgetType.valueOf(gadgetType)));
                if (commands[++count].equals("null")) {
                    break;
                }
            }
        } else {
            if(appInFocus) {
                writeToast("No gadgets in your system");
            }
        }
        if(appInFocus) {
            currentFragment.update(null);
        }
    }

    // #20
    private void lostServerConnection() {
        writeToast("No server connection");
        getSupportActionBar().hide();
        if (loginAttemptWithSessionKey) {
            fragmentTransaction("login");
        } else {
            fragmentTransaction("setup");
        }
    }

    // ========================== TOOLBAR MENU ===========================================

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        Fragment fragment = null;
        String tag = null;

        switch (id) {
            case R.id.home:
                fragment = new HomeFragment();
                tag = "home";
                break;
            case R.id.settings:
                fragment = new SettingsFragment();
                tag = "settings";
                break;
            case R.id.about:
                fragment = new AboutFragment();
                tag = "about";
                break;
            case R.id.logout:
                fragment = new LogoutFragment();
                tag = "logoutBtn";
                break;
            default:
                return false;
        }
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.frame_layout, fragment, tag);
        //fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
        return true;
    }

    // =========================== UTILITY METHODS ======================================

    public void writeToast(String message) {
        // Display short messages to the client.
        Context context = getApplicationContext();
        int duration = Toast.LENGTH_SHORT;

        Toast toast = Toast.makeText(context, message, duration);
        toast.show();
    }

    public void fragmentTransaction(String fragment) {
        Fragment fragmentToLoad = null;
        String tag = null;
        switch (fragment) {
            case "login":
                fragmentToLoad = new LoginFragment();
                tag = "login";
                break;
            case "logoutBtn":
                fragmentToLoad = new LogoutFragment();
                tag = "logoutBtn";
                break;
            case "home":
                fragmentToLoad = new HomeFragment();
                tag = "home";
                break;
            case "settings":
                fragmentToLoad = new SettingsFragment();
                tag = "settings";
                break;
            case "about":
                fragmentToLoad = new AboutFragment();
                tag = "about";
                break;
            case "setup":
                fragmentToLoad = new SetupFragment();
                tag = "setup";
                break;
            default:
                writeToast("Invalid Parameters");
        }
        try {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.frame_layout, fragmentToLoad, tag);
            transaction.commit();
        } catch (NullPointerException e) {
            writeToast("Invalid parameters");
        }
    }

    public void writeUserToCache() {

        new Thread(new Runnable() {
            @Override
            public void run() {
                String filePath = getCacheDir() + "userdata";
                try (ObjectOutputStream objectOutput = new ObjectOutputStream(new FileOutputStream(new File(filePath)))) {
                    objectOutput.writeObject(loggedInUser);
                } catch (IOException e) {
                    writeToast("Unable to write user tho cache");
                }
            }
        }).start();
    }

    private void writeServerSpecToCache() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String filePath = getCacheDir() + "serverSpec";
                try (ObjectOutputStream objectOutput = new ObjectOutputStream(new FileOutputStream(new File(filePath)))) {
                    objectOutput.writeObject(server);
                } catch (IOException e) {
                    writeToast("Unable to write server spec tho cache");
                }
            }
        }).start();

    }

    private void readServerSpecFromCache() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean successfulCacheRead = false;
                try (ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(new File(getCacheDir() + "serverSpec")))) {

                    server = (ServerSpec) objectInputStream.readObject();
                    successfulCacheRead = true;

                } catch (Exception e) {
                    successfulCacheRead = false;
                } finally {
                    postResult(successfulCacheRead);
                }
            }
            private void postResult(final boolean successfulCacheRead) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        // Process result of cache read operation
                        serverDefined = successfulCacheRead;
                        if(serverDefined) {
                            fragmentTransaction("setup");
                        } else {
                            fragmentTransaction("login");
                        }
                    }
                });
            }
        }).start();
    }
}