package main.gadgets.automations;

import main.Server;

import java.time.LocalTime;
import java.util.ArrayList;

public class Automation_Time extends Automation {
    private final LocalTime triggerTime;
    private boolean hasExecuted;


    public Automation_Time(String name, Timer timer, ArrayList<Action> actions, int hour, int minute) {
        super(name, timer, actions);
        triggerTime = LocalTime.of(hour, minute);
        hasExecuted = false;
    }

    @Override
    public void runScan(int... args) {
        if (triggerMatch()) {
            timer.start();
        }
        if (timer.isRunning && timer.timeOut()) {
            executeActions();
            timer.stop();
        }
    }

    private boolean triggerMatch() {
        LocalTime now = LocalTime.now();
        if (now.isAfter(triggerTime)) {
            if (hasExecuted) {
                return false;
            } else {
                hasExecuted = true;
                return true;
            }
        } else {
            hasExecuted = false;
            return false;
        }
    }

    @Override
    protected void executeActions() {
        for (Action action : actions) {
            try {
                Server.getInstance().requestsToHomeServer.put(action.toAlmaString());
            } catch (InterruptedException e) {
                System.out.println("Unable to launch automation action");
            }
        }
    }
}
