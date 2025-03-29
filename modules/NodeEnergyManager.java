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

        Random rand = new Random(nodeId); // deterministik per node
        this.initialEnergy = minEnergy + (maxEnergy - minEnergy) * rand.nextDouble();
        this.energyLevel = this.initialEnergy;
    }

    public void setDebugMode(boolean enabled) {
        this.debugMode = enabled;
    }

    public void setEnergyEventListener(EnergyEventListener listener) {
        this.energyListener = listener;
    }

    public void consume(double energy) {
        energyLevel -= energy;

        if (energyLevel < 0) {
            energyLevel = 0;

            if (debugMode) {
                System.out.println("[EnergyManager] Energy depleted to 0");
            }

            // Trigger event listener
            if (energyListener != null) {
                energyListener.onBatteryDepleted(this.nodeId);
            }
        } else if (debugMode) {
            System.out.println("[EnergyManager] Energy after consume: " + energyLevel);
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
}
