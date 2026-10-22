package dev.sh3lby.antiillegalenchants.action;

public final class ScanReport {
    private int checked;
    private int illegal;

    public void checked() {
        checked++;
    }

    public void illegal() {
        illegal++;
    }

    public int checkedCount() {
        return checked;
    }

    public int illegalCount() {
        return illegal;
    }
}
