package com.example.androidclient.utilities;

import java.io.Serializable;

public class LoggedInUser implements Serializable {
    // Set at successful login.
    // Stored to cache at successful login, to be used for automatic login next time the app
    // is launched or brought to focus.

    private String name;
    private boolean admin;
    private String systemName;
    private String sessionKey;

    public LoggedInUser(String name, boolean admin, String systemName, String sessionKey) {
        this.name = name;
        this.admin = admin;
        this.systemName = systemName;
        this.sessionKey = sessionKey;
    }

    public String getName() {
        return name;
    }

    public boolean isAdmin() {
        return admin;
    }

    public String getSystemName() {
        return systemName;
    }

    public String getSessionKey() {
        return sessionKey;
    }

    /*public void setSessionKey(String sessionKey) {
        this.sessionKey = sessionKey;
    }*/
}
