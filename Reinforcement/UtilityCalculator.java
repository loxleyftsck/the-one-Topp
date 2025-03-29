package Reinforcement;

import core.DTNHost;

public class UtilityCalculator {
    private double weightAbility;
    private double weightSocial;

    public UtilityCalculator(core.Settings settings) {
        // Default weights, bisa diganti dari settings
        this.weightAbility = settings.contains("Utility.abilityWeight") ? settings.getDouble("Utility.abilityWeight") : 0.5;
        this.weightSocial = settings.contains("Utility.socialWeight") ? settings.getDouble("Utility.socialWeight") : 0.5;
    }

    public double calculateTOPP(double battery, double buffer, int popularity, double tieStrength, double density) {
        double ability = 0.5 * battery + 0.5 * (1.0 - buffer); // Semakin kecil buffer, semakin baik
        double normPop = normalize(popularity);
        double social = 0.5 * normPop + 0.5 * tieStrength;
        double normDensity = normalize(density);

        double weightedUtility = weightAbility * ability + weightSocial * social;
        double topp = 0.8 * weightedUtility + 0.2 * normDensity;

        System.out.println("[UtilityCalculator] TOPP = " + topp +
                " | Battery = " + battery +
                ", Buffer = " + buffer +
                ", Popularity = " + popularity +
                ", TieStrength = " + tieStrength +
                ", Density = " + density);

        return topp;
    }

    private double normalize(double val) {
        return Math.min(1.0, Math.max(0.0, val / 10.0));
    }
}
