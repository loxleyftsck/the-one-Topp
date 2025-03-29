package report;

import core.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter; // ← ini yang belum ada
import java.util.HashMap;
import java.util.Map;


public class CopySummaryReport extends Report {
    private final Map<String, Integer> messageCopies = new HashMap<>();
    private int totalMessages = 0;
    private int totalDelivered = 0;
    public static CopySummaryReport reportInstance = null;


    public void messageCreated(String id) {
        totalMessages++;
        messageCopies.put(id, 0);
    }
    public CopySummaryReport() {
        reportInstance = this;
    }


    public void messageCopied(String id) {
        messageCopies.merge(id, 1, Integer::sum);
    }

    public void messageDelivered(String id) {
        totalDelivered++;
    }
    @Override
    public void done() {
        CopyMonitor cm = CopyMonitor.getInstance();
        try {
            File dir = new File("reports");
            if (!dir.exists()) dir.mkdirs();

            PrintWriter writer = new PrintWriter(new File("reports/copy_summary.txt"));
            writer.println("Total Messages: " + cm.getTotalMessages());
            writer.println("Messages Delivered: " + cm.getMessagesDelivered());
            writer.println("Total Copies: " + cm.getTotalCopies());
            writer.println("Average Copies per Message: " + cm.getAverageCopies());
            writer.close();

            System.out.println("[CopySummaryReport] Summary written to reports/copy_summary.txt");
        } catch (IOException e) {
            System.err.println("[CopySummaryReport] Failed to write summary");
            e.printStackTrace();
        }
    }



}
