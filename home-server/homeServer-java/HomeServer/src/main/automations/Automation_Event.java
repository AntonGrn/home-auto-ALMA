package main.automations;

import main.Server;
import java.util.ArrayList;

public class Automation_Event extends Automation {
    private final int triggerGadgetID;
    private final int triggerState;
    private final StateCondition stateCondition;

    public Automation_Event(String name, Timer timer, ArrayList<Action> actions, int triggerGadgetID, int triggerState, StateCondition stateCondition) {
        super(name, timer, actions);
        this.triggerGadgetID = triggerGadgetID;
        this.triggerState = triggerState;
        this.stateCondition = stateCondition;
    }

    @Override
    public void runScan(int... args) {
        if (args.length == 2) {
            int eventGadgetID = args[0];
            int eventState = args[1];
            if (eventGadgetID == triggerGadgetID) {
                if (triggerMatch(eventState)) {
                    timer.start();
                } else {
                    timer.stop();
                }
            }
        }
        if (timer.isRunning && timer.timeOut()) {
            executeActions();
            timer.stop();
        }
    }

    private boolean triggerMatch(int eventState) {
        switch (stateCondition) {
            case EQUAL_TO:
                return (eventState == triggerState);
            case GREATER_THAN:
                return (eventState > triggerState);
            case LESS_THAN:
                return (eventState < triggerState);
            case GREATER_OR_EQUAL_TO:
                return (eventState >= triggerState);
            case LESS_OR_EQUAL_TO:
                return (eventState <= triggerState);
        }
        return false;
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
