package Reinforcement;

import core.DTNHost;

import java.util.List;

public class NetworkEnvironment {
    public ContextState senseContext(double battery, double buffer, int popularity, double tieStrength) {
        return new ContextState(battery, buffer, popularity, tieStrength);
    }

    public double calculateDensity(DTNHost self, List<DTNHost> allHosts, double range) {
        int nearby = 0;
        for (DTNHost other : allHosts) {
            if (other == self) continue;
            double distance = self.getLocation().distance(other.getLocation());
            if (distance <= range) {
                nearby++;
            }
        }
        return (double) nearby / (allHosts.size() - 1);
    }
}
