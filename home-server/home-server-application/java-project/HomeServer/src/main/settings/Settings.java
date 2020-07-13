package main.settings;

public class Settings {
    // Settings variables set from 'config.json' at system boot.
    public boolean debugMode;
    public boolean remoteAccess;
    public String hubAlias;
    public int hubID;
    private String hubPwd;
    public String publServerIP;
    public int publServerPort;

    public Settings(boolean debugMode, boolean remoteAccess, String hubAlias, int hubID, String hubPwd, String publServerIP, int publServerPort) {
        this.debugMode = debugMode;
        this.remoteAccess = remoteAccess;
        this.hubAlias = hubAlias;
        if(remoteAccess) {
            this.hubID = hubID;
            this.hubPwd = hubPwd;
            this.publServerIP = publServerIP;
            this.publServerPort = publServerPort;
        }
    }

    public String getHubPwd() {
        return hubPwd;
    }

    public void clearHubPwd() {
        hubPwd = "";
    }
}
