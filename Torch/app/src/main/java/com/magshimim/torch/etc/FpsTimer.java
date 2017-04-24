package com.magshimim.torch.etc;

public class FpsTimer {
    private long last;

    public FpsTimer() {
        last = System.currentTimeMillis();
    }

    public void start() {
        last = System.currentTimeMillis();
    }

    public double sampleFps() {
        long curr = System.currentTimeMillis();
        long delta = curr - last;
        last = curr;
        return 1000.0 / delta;
    }
}
