package report;


public class CopyMonitor {
    private static CopyMonitor instance = new CopyMonitor();
    private int totalMessages = 0;
    private int messagesDelivered = 0;
    private int totalCopies = 0;

    private CopyMonitor() {}

    public static CopyMonitor getInstance() {
        return instance;
    }

    public void registerMessage(String id) {
        totalMessages++;
    }

    public void registerCopy(String id) {
        totalCopies++;
    }

    public void messageDelivered(String id) {
        messagesDelivered++;
    }

    public int getTotalMessages() {
        return totalMessages;
    }

    public int getMessagesDelivered() {
        return messagesDelivered;
    }

    public int getTotalCopies() {
        return totalCopies;
    }

    public double getAverageCopies() {
        return totalMessages == 0 ? 0.0 : (double) totalCopies / totalMessages;
    }
}
