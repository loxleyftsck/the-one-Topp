package modules;

import java.util.Random;

public class NodeEnergyManager {
    private int nodeId;
    private double energyLevel;
    private double initialEnergy;

    private boolean debugMode = false;
    private EnergyEventListener energyListener;

    public NodeEnergyManager(int nodeId, double minEnergy, double maxEnergy) {
        this.nodeId = nodeId;

        // Energi awal deterministik per node (reproducible)
        Random rand = new Random(nodeId);
        this.initialEnergy = minEnergy + (maxEnergy - minEnergy) * rand.nextDouble();
        this.energyLevel = this.initialEnergy;

        if (debugMode) {
            System.out.printf("[EnergyManager] Node %d initialized with energy: %.2f J\n", nodeId, energyLevel);
        }
    }

    public void setDebugMode(boolean enabled) {
        this.debugMode = enabled;
    }

    public void setEnergyEventListener(EnergyEventListener listener) {
        this.energyListener = listener;
    }

    /**
     * Mengonsumsi sejumlah energi. Jika energi habis, trigger listener.
     */
    public void consume(double amount) {
        if (amount <= 0) return; // Ignore konsumsi nol atau negatif

        energyLevel -= amount;

        if (energyLevel <= 0) {
            energyLevel = 0;

            if (debugMode) {
                System.out.printf("[EnergyManager] Node %d energy depleted to 0 J\n", nodeId);
            }

            if (energyListener != null) {
                energyListener.onBatteryDepleted(this.nodeId);
            }
        } else if (debugMode) {
            System.out.printf("[EnergyManager] Node %d energy after consume: %.2f J\n", nodeId, energyLevel);
        }
    }

    /**
     * Recharge energi secara manual (jika kamu mau simulasikan charging)
     */
    public void recharge(double amount) {
        if (amount > 0) {
            energyLevel += amount;
            if (energyLevel > initialEnergy) {
                energyLevel = initialEnergy;
            }

            if (debugMode) {
                System.out.printf("[EnergyManager] Node %d recharged to %.2f J\n", nodeId, energyLevel);
            }
        }
    }

    public double getEnergyLevel() {
        return energyLevel;
    }

    public double getInitialEnergy() {
        return initialEnergy;
    }

    public double getNormalizedEnergy() {
        return initialEnergy > 0 ? energyLevel / initialEnergy : 0.0;
    }

    public int getNodeId() {
        return nodeId;
    }

    /**
     * Cek apakah node bisa bertindak (default 100J)
     */
    public boolean canAct(double threshold) {
        return energyLevel >= threshold;
    }
}
