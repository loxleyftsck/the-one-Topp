package report;

import Reinforcement.ContextState;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.io.File;

/**
 * QTableReport digunakan sebagai logger eksternal untuk menyimpan isi Q-Table
 * ke dalam file status, bukan sebagai report resmi The ONE.
 */
public class QTableReport {
    public static void saveQTableToFile(Map<ContextState, Map<String, Double>> qTable, String nodeId) {
        String filename = "qtable_node_" + nodeId + "_status.txt";

        try (FileWriter writer = new FileWriter(filename)) {
            writer.write("[QTableReport] Node " + nodeId + " Q-Table Size: " + qTable.size() + "\n");
            if (qTable.isEmpty()) {
                writer.write("[QTableReport] Q-Table is empty. No learning has occurred yet.\n");
            } else {
                writer.write("[QTableReport] Q-Table contains entries:\n");
                for (Map.Entry<ContextState, Map<String, Double>> entry : qTable.entrySet()) {
                    String state = entry.getKey().toString().replaceAll(",", ";");
                    for (Map.Entry<String, Double> actionEntry : entry.getValue().entrySet()) {
                        writer.write("State: " + state + " | Action: " + actionEntry.getKey() + " = " + actionEntry.getValue() + "\n");
                    }
                }
            }
            System.out.println("[QTableReport] File saved to: " + new File(filename).getAbsolutePath());
        } catch (IOException e) {
            System.err.println("[QTableReport] Error writing file: " + e.getMessage());
        }
    }
}