package main.automations;

import java.util.ArrayList;

public abstract class Automation {
    public String name;
    public Timer timer;
    public ArrayList<Action> actions;

    public Automation(String name, Timer timer, ArrayList<Action> actions) {
        this.name = name;
        this.timer = timer;
        this.actions = actions;
    }

    public abstract void runScan(int... args);
    protected abstract void executeActions();

}
