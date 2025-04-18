package Reinforcement;

import java.util.Objects;

/**
 * ContextState merepresentasikan state lingkungan untuk Reinforcment learning
 * termasuk informasi baterai, buffer, popularitas, tie strength, dan density (opsional).
 */
public class ContextState {
    private double battery;
    private double buffer;
    private int popularity;
    private double tieStrength;
    private double density; // NEW: Real-time node density

    // Constructor dengan density
    public ContextState(double battery, double buffer, int popularity, double tieStrength, double density) {
        this.battery = round(battery);
        this.buffer = round(buffer);
        this.popularity = popularity;
        this.tieStrength = round(tieStrength);
        this.density = round(density);
    }

    // Constructor tanpa density (opsional)
    public ContextState(double battery, double buffer, int popularity, double tieStrength) {
        this(battery, buffer, popularity, tieStrength, 0.0);
    }

    public double getBattery() {
        return battery;
    }

    public double getBuffer() {
        return buffer;
    }

    public int getPopularity() {
        return popularity;
    }

    public double getTieStrength() {
        return tieStrength;
    }

    public double getDensity() {
        return density;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
    public String toCSV() {
        return battery + "," + buffer + "," + popularity + "," + tieStrength + "," + density;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ContextState)) return false;
        ContextState that = (ContextState) o;
        return Double.compare(that.battery, battery) == 0 &&
                Double.compare(that.buffer, buffer) == 0 &&
                popularity == that.popularity &&
                Double.compare(that.tieStrength, tieStrength) == 0 &&
                Double.compare(that.density, density) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(battery, buffer, popularity, tieStrength, density);
    }

    @Override
    public String toString() {
        return "Context[" +
                "batt=" + battery +
                ", buf=" + buffer +
                ", pop=" + popularity +
                ", tie=" + tieStrength +
                ", den=" + density +
                ']';
    }
}
