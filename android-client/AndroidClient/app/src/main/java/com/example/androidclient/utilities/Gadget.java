package com.example.androidclient.utilities;

public class Gadget {
    private final int gadgetID;
    private final String name;
    private int state;
    private final GadgetType type;

    public Gadget(int gadgetID, String nameID, int value, GadgetType type) {
        this.gadgetID = gadgetID;
        this.name = nameID;
        this.state = value;
        this.type = type;
    }

    public int getGadgetID() {
        return gadgetID;
    }

    public String getName() {
        return name;
    }

    public int getState() {
        return state;
    }

    public GadgetType getType() {
        return type;
    }
}
