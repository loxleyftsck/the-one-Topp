package report;

import core.*;
import routing.RLRouter;
import Reinforcement.ContextState;
import Reinforcement.QLearning;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

public class FinalQTableDump extends Report {

    @Override
    public void done() {
        System.out.println("[FinalQTableDump] Dumping Q-tables to file...");

        for (DTNHost host : SimScenario.getInstance().getHosts()) {
            if (!(host.getRouter() instanceof RLRouter rlRouter)) continue;
            QLearning q = rlRouter.getQLearning();
            Map<ContextState, Map<String, Double>> qTable = q.getQTable();

            if (qTable.isEmpty()) {
                System.out.println("[FinalQTableDump] Node " + host.getAddress() + " has empty Q-table.");
                continue;
            }

            String fileName = "qtable_node_" + host.getAddress() + ".csv";
            try (FileWriter fw = new FileWriter(fileName)) {
                fw.write("battery,buffer,popularity,tieStrength,density,action,qValue\n");
                for (ContextState state : qTable.keySet()) {
                    for (Map.Entry<String, Double> entry : qTable.get(state).entrySet()) {
                        fw.write(state.toCSV() + "," + entry.getKey() + "," + entry.getValue() + "\n");
                    }
                }
                System.out.println("[FinalQTableDump] Wrote Q-table for node " + host.getAddress());
            } catch (IOException e) {
                System.err.println("[FinalQTableDump] Error writing file: " + fileName);
                e.printStackTrace();
            }
        }
    }
}
