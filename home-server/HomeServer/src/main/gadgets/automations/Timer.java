package main.gadgets.automations;

public class Timer {
    private long delayActionMs;
    private long systemTimeToExecute;
    public boolean isRunning;

    public Timer (int hours, int minutes, int seconds) {
        delayActionMs = (hours*60*60*1000) + (minutes*60*1000) + (seconds*1000);
        isRunning = false;
    }

    public void start() {
        long currentMillis = System.currentTimeMillis();
        systemTimeToExecute = currentMillis + delayActionMs;
        isRunning = true;
    }

    public void stop() {
        isRunning = false;
    }

    public boolean timeOut() {
        long currentMillis = System.currentTimeMillis();
        return currentMillis >= systemTimeToExecute;
    }

}
