package report;



import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Report untuk mencatat aksi copy yang terjadi dalam simulasi:
 * mencatat ID pesan, node asal, node tujuan, nilai TOPP, nilai Q-value, dan waktu.
 */
public class CopyActionLogReport extends Report {

    public static CopyActionLogReport instance;

    private List<String> logLines = new ArrayList<>();

    public CopyActionLogReport() {
        instance = this;
    }

    /**
     * Panggil method ini saat pesan disalin.
     */
    public void logCopy(String msgId, int from, int to, double topp, double qval, double time) {
        logLines.add(msgId + "\t" + from + "\t" + to + "\t" + topp + "\t" + qval + "\t" + time);
    }

    @Override
    public void done() {
        try {
            File dir = new File("reports");
            if (!dir.exists()) {
                dir.mkdirs();
            }

            PrintWriter writer = new PrintWriter("reports/copy_action_log.txt");
            writer.println("msgId\tfrom\tto\ttopp\tqval\ttime");

            for (String line : logLines) {
                writer.println(line);
            }

            writer.close();
            System.out.println("[CopyActionLogReport] Log saved to reports/copy_action_log.txt");
        } catch (IOException e) {
            System.err.println("[CopyActionLogReport] Failed to write copy action log.");
            e.printStackTrace();
        }
    }
}
