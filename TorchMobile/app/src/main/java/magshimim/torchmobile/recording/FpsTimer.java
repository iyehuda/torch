package magshimim.torchmobile.recording;

class FpsTimer {
    private int delta;
    private long last;

    FpsTimer(int fps) {
        if(fps <= 0)
            throw new IllegalArgumentException("Invalid fps");
        delta = 1000 / fps;
        set();
    }

    void set() {
        last = System.currentTimeMillis();
    }

    boolean check() {
        long curr = System.currentTimeMillis();
        if(curr - last < delta)
            return false;
        last = curr;
        return true;
    }
}
