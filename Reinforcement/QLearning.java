package Reinforcement;


import core.SimClock;
import java.util.Random; // untuk pemilihan aksi eksploratif
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class QLearning {
    private Map<ContextState, Map<String, Double>> qTable;
    private List<String> actions;
    private double alpha; // learning rate
    private double gamma; // discount factor
    private double epsilon; // exploration rate

    private double decayRate = 0.99;
    private double decayInterval = 1000.0;
    private double lastDecayTime = 0.0;


    public QLearning(List<String> actions, double alpha, double gamma, double epsilon) {
        this.actions = actions;
        this.alpha = alpha;
        this.gamma = gamma;
        this.epsilon = epsilon;
        this.qTable = new HashMap<>();

    }


    public void logQTable(int nodeId) {
        if (qTable == null || qTable.isEmpty()) {
            System.out.println("[QTable] Q-Table kosong untuk node ini.");
            return;
        }

        String filename = "qtable_node_" + nodeId + "_status.txt";

        try (PrintWriter writer = new PrintWriter(new FileWriter(filename, false))) {
            writer.println("State,Action,Q-Value");

            for (Map.Entry<ContextState, Map<String, Double>> entry : qTable.entrySet()) {
                ContextState state = entry.getKey();
                Map<String, Double> actionMap = entry.getValue();

                for (Map.Entry<String, Double> actionEntry : actionMap.entrySet()) {
                    writer.printf("%s,%s,%.4f%n", state.toCSV(), actionEntry.getKey(), actionEntry.getValue());
                }
            }

            System.out.println("[QTable] Dump Q-Table node " + nodeId + " ke " + filename);
        } catch (IOException e) {
            System.err.println("[QTable] Gagal menulis Q-Table: " + e.getMessage());
        }
    }


    public void update(ContextState state, String action, double reward, ContextState nextState) {
        if (!qTable.containsKey(state)) {
            qTable.put(state, new HashMap<>());
        }

        Map<String, Double> stateActions = qTable.get(state);
        double currentQ = stateActions.getOrDefault(action, 0.0);

        double maxQ = 0.0;
        if (qTable.containsKey(nextState) && !qTable.get(nextState).isEmpty()) {
            maxQ = Collections.max(qTable.get(nextState).values());
        }

        double newQ = currentQ + alpha * (reward + gamma * maxQ - currentQ);
        if (newQ < 0.001) newQ = 0.0;

        stateActions.put(action, newQ);
        qTable.put(state, stateActions);

        System.out.println("[Q-UPDATE] " + state + " - Action: " + action + " - Q: " + currentQ + " -> " + newQ);

        double currentTime = SimClock.getTime();
        if (currentTime - lastDecayTime >= decayInterval) {
            decayAllQValues();
            lastDecayTime = currentTime;
        }
    }

    public String chooseAction(ContextState state) {
        if (!qTable.containsKey(state) || Math.random() < epsilon) {
            String randomAction = actions.get(new Random().nextInt(actions.size()));
            System.out.println("[RL] Choosing action for state: " + state);
            System.out.println("[Q-ACTION] Exploring: " + randomAction);
            return randomAction;
        } else {
            Map<String, Double> stateActions = qTable.get(state);
            String bestAction = Collections.max(stateActions.entrySet(), Map.Entry.comparingByValue()).getKey();
            System.out.println("[Q-ACTION] Exploiting: " + bestAction);
            return bestAction;
        }
    }

    public double getQValue(ContextState state, String action) {
        Map<String, Double> stateActions = qTable.getOrDefault(state, new HashMap<>());
        return stateActions.getOrDefault(action, 0.0);
    }

    public void decayAllQValues() {
        for (ContextState state : qTable.keySet()) {
            Map<String, Double> actionsMap = qTable.get(state);
            for (String action : actionsMap.keySet()) {
                double oldQ = actionsMap.get(action);
                double newQ = oldQ * decayRate;
                actionsMap.put(action, newQ);
                System.out.println("[Q-Decay] State: " + state + " | Action: " + action + " | Q: " + oldQ + " -> " + newQ);
            }
        }
    }

    public Map<ContextState, Map<String, Double>> getQTable() {
        return this.qTable;
    }

    public void syncWith(Map<ContextState, Map<String, Double>> otherQTable) {
        for (Map.Entry<ContextState, Map<String, Double>> entry : otherQTable.entrySet()) {
            ContextState state = entry.getKey();
            Map<String, Double> otherActions = entry.getValue();

            this.qTable.putIfAbsent(state, new HashMap<>());
            Map<String, Double> myActions = this.qTable.get(state);

            for (Map.Entry<String, Double> actionEntry : otherActions.entrySet()) {
                String action = actionEntry.getKey();
                double theirQ = actionEntry.getValue();
                double ourQ = myActions.getOrDefault(action, 0.0);

                myActions.put(action, Math.max(theirQ, ourQ));
            }
        }
    }
}
