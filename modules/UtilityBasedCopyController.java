package modules;

import java.util.*;

public class UtilityBasedCopyController {

    // Menyimpan jumlah salinan tersisa untuk setiap pesan
    private Map<String, Integer> messageCopies = new HashMap<>();

    // Nilai default maksimum salinan (L)
    private int defaultMaxCopies = 5;

    /**
     * Dipanggil saat pesan baru dibuat di node sumber
     */
    public void registerNewMessage(String msgId) {
        messageCopies.put(msgId, defaultMaxCopies);
    }

    /**
     * Mengambil jumlah salinan saat ini dari suatu pesan
     */
    public int getCopies(String msgId) {
        return messageCopies.getOrDefault(msgId, defaultMaxCopies);
    }

    /**
     * Menghapus informasi salinan setelah pesan terkirim/dihapus
     */
    public void remove(String msgId) {
        messageCopies.remove(msgId);
    }

    /**
     * 🔹 PENGGERAK KUALITAS: Mengevaluasi apakah node lain layak menerima pesan
     * Berdasarkan perbandingan nilai Utility dan Q-Value
     */
    public boolean canForward(String msgId, double myUtility, double peerUtility, double myQ, double peerQ, boolean isDest) {
        if (isDest) return true; // Jika node tujuan, langsung kirim
        return peerUtility > myUtility || peerQ > myQ;
    }

    /**
     * 🔹 PENGGERAK KUANTITAS: Hitung batas L_copy adaptif berdasarkan densitas
     */
    public int calculateAdaptiveLimit(int L_cur, int L_max, Set<Integer> tensI, Set<Integer> tensJ, int totalNodes) {
        Set<Integer> union = new HashSet<>(tensI);
        union.addAll(tensJ);

        double density = (double) union.size() / totalNodes;

        // Jika density rendah, adaptiveL mendekati L_max
        int adaptiveL = (int) Math.floor(density * L_max);

        // Return nilai terkecil antara L_cur dan adaptif, minimal 1
        return Math.min(L_cur, Math.max(1, adaptiveL));
    }

    /**
     * 🔹 Mengurangi jumlah salinan saat membagi separuh salinan ke node lain
     */
    public void forwardHalf(String msgId) {
        int current = messageCopies.getOrDefault(msgId, defaultMaxCopies);
        int half = Math.max(1, current / 2);
        messageCopies.put(msgId, current - half);

        // Tambahkan pelaporan salinan
        report.CopyMonitor.getInstance().registerCopy(msgId);
    }

    /**
     * 🔹 Menghabiskan salinan terakhir untuk pengiriman final
     */
    public void forwardFinal(String msgId) {
        messageCopies.put(msgId, 0);

        // Tambahkan pelaporan salinan
        report.CopyMonitor.getInstance().registerCopy(msgId);
    }


    /**
     * Untuk logging atau debug jika dibutuhkan
     */
    public void printCopies() {
        System.out.println("[CopyControl] Current Copy Table: ");
        for (Map.Entry<String, Integer> e : messageCopies.entrySet()) {
            System.out.println("Message " + e.getKey() + " : " + e.getValue() + " copies");
        }
    }
}
