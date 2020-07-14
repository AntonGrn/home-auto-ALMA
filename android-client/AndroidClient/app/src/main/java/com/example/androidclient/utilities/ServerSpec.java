package com.example.androidclient.utilities;

import java.io.Serializable;

public class ServerSpec implements Serializable {
    // Written to cache upon successful login (for automatic use next time).
    public final String IP;
    public final int port;

    public ServerSpec(String IP, int port) {
        this.IP = IP;
        this.port = port;
    }
}
