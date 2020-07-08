package main.automations;

import java.util.ArrayList;

public class AutomationHandler {
    private ArrayList<Automation> automations;
    private final Object lock_event;
    private final Object lock_time;

    public AutomationHandler(ArrayList<Automation> automations) {
        this.automations = automations;
        lock_event = new Object();
        lock_time = new Object();
    }

    // Check automation triggers when a gadget has altered state.
    public void newEvent(int eventGadgetID, int eventState) {
        synchronized (lock_event) {
            for (Automation automation : automations) {
                if (automation instanceof Automation_Event) {
                    automation.runScan(eventGadgetID, eventState);
                }
            }
        }
    }

    // Periodically check automation triggers and timers.
    public void runTimeScan() {
        synchronized (lock_time) {
            for (Automation automation : automations) {
                automation.runScan();
            }
        }
    }
}
