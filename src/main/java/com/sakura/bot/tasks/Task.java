package com.sakura.bot.tasks;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class Task extends TimerTask {
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final int loopTime;
    private final int delay;
    private static final long MILLI_TO_MIN_COF = 60000;

    public Task(int loopTime, int delay) {
        this.loopTime = loopTime;
        this.delay = delay;
    }

    boolean isNotRunning() {
        return !running.get();
    }

    void scheduleTask() {
        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(this, delay * MILLI_TO_MIN_COF,
            loopTime * MILLI_TO_MIN_COF);
    }

    public abstract void execute();

    @Override
    public void run() {
        running.set(true);
        execute();
    }
}
