package Reinforcement;

public class RewardFunction {
    public double computeBinaryReward(boolean delivered) {
        return delivered ? 1.0 : 0.0;
    }

    public double computeGradedReward(boolean delivered, double delay) {
        if (delivered && delay < 600) return 1.0;
        else if (delivered) return 0.5;
        else return 0.0;
    }
}
