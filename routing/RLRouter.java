/* RLRouter.java - Patched for battery-aware message transfer */

package routing;

import core.*;
import Reinforcement.*;
import report.CopyMonitor;
import modules.NodeEnergyManager;
import report.CopySummaryReport;
import report.QTableReport;
import report.CopyActionLogReport;
import modules.UtilityBasedCopyController;

import java.util.*;

public class RLRouter extends ActiveRouter {
    private Settings settings;
    private NodeEnergyManager energyManager;

    private Set<Integer> uniqueContacts;
    private Map<Integer, Double> contactDurations;
    private double observationStartTime;

    private Map<Integer, Double> tieStrengthMap;
    private Map<Connection, Double> connectionStartTimes;

    private boolean isDead = false;
    private QLearning agent;
    private List<String> actions = Arrays.asList("FORWARD", "DROP", "STORE");
    private ContextState lastState;

    private UtilityBasedCopyController copyControl = new UtilityBasedCopyController();
    private static List<DTNHost> allHosts = null;

    private boolean enableQTableReport = true;
    private int qtableDumpInterval = 1000;
    private double lastTOPP = 0.0;

    private double computeReceiveCost(Message msg) {
        return 200.0 + msg.getSize() / 1000.0;
    }

    private double computeSendCost(Message msg) {
        return 300.0 + msg.getSize() / 500.0;
    }

    public static void setAllHosts(List<DTNHost> hosts) {
        allHosts = hosts;
    }

    public RLRouter(Settings s) {
        super(s);
        this.settings = s;
        this.enableQTableReport = settings.contains("QTableReport.enable") ? settings.getBoolean("QTableReport.enable") : true;
        this.qtableDumpInterval = settings.contains("QTableReport.interval") ? settings.getInt("QTableReport.interval") : 1000;
        System.out.println("[RLRouter] Konstruktor dipanggil.");
    }

    protected RLRouter(RLRouter r) {
        super(r);
        this.settings = r.settings;
        this.energyManager = r.energyManager;
        this.enableQTableReport = r.enableQTableReport;
        this.qtableDumpInterval = r.qtableDumpInterval;
    }

    @Override
    public MessageRouter replicate() {
        return new RLRouter(this);
    }

    @Override
    public void initialize(DTNHost host, List<MessageListener> mListeners) {
        super.initialize(host, mListeners);

        if (this.energyManager == null) {
            this.energyManager = new NodeEnergyManager(host.getAddress(), 8000, 10000);
        }

        this.energyManager.setDebugMode(true);
        this.energyManager.setEnergyEventListener(nodeId -> {
            System.out.println("[Event] Node " + nodeId + " kehabisan energi!");
            isDead = true;
        });

        this.agent = new QLearning(actions, 0.1, 0.9, 0.2);
        this.uniqueContacts = new HashSet<>();
        this.contactDurations = new HashMap<>();
        this.tieStrengthMap = new HashMap<>();
        this.connectionStartTimes = new HashMap<>();
        this.observationStartTime = SimClock.getTime();

        System.out.println("[RLRouter] Inisialisasi selesai untuk node " + host.getAddress());
    }

    @Override
    public void update() {
        if (settings.contains("RL.logNodeUpdate") && settings.getBoolean("RL.logNodeUpdate")) {
            System.out.println("[DEBUG] Node " + getHost().getAddress() + " is updating.");
        }

        super.update();

        if (allHosts == null) {
            allHosts = SimScenario.getInstance().getWorld().getHosts();
            System.out.println("[RLRouter] allHosts initialized from SimScenario (via update)");
        }

        if (isDead) return;

        updateContext();
        logQTableIfNeeded();
        checkDeliveredMessages();
    }

    private void updateContext() {

        double current = energyManager.getEnergyLevel();
        double max = energyManager.getInitialEnergy();
        double battery = energyManager.getNormalizedEnergy();

        double totalBuffer = getBufferSize();
        double usedBuffer = totalBuffer - getFreeBufferSize();
        double buffer = totalBuffer > 0 ? usedBuffer / totalBuffer : 0.0;

        double currentTime = SimClock.getTime();

        for (Connection con : getHost().getConnections()) {
            System.out.println("[DEBUG] Active connections: " + getHost().getConnections().size());
            System.out.println("[DEBUG] Node " + getHost().getAddress() + " connections: " + getHost().getConnections().size());


            DTNHost other = con.getOtherNode(getHost());
            int otherId = other.getAddress();

            uniqueContacts.add(otherId);
            connectionStartTimes.putIfAbsent(con, currentTime);
            double startTime = connectionStartTimes.get(con);
            double duration = currentTime - startTime;

            contactDurations.put(otherId, contactDurations.getOrDefault(otherId, 0.0) + duration);
            double totalObservationTime = currentTime - observationStartTime;
            double tieStrength = totalObservationTime > 0 ? contactDurations.get(otherId) / totalObservationTime : 0.0;
            tieStrengthMap.put(otherId, tieStrength);

            connectionStartTimes.put(con, currentTime);
        }

        int popularity = uniqueContacts.size();

        for (Map.Entry<Integer, Double> entry : tieStrengthMap.entrySet()) {
            int neighborId = entry.getKey();
            double tieStrength = entry.getValue();

            NetworkEnvironment env = new NetworkEnvironment();
            double density = allHosts != null ? env.calculateDensity(getHost(), allHosts, 100.0) : 0.0;
            System.out.println("[RLRouter] Current density: " + density);

            ContextState state = new ContextState(battery, buffer, popularity, tieStrength);
            lastState = state;

            UtilityCalculator calc = new UtilityCalculator(this.settings);
            RewardFunction rewarder = new RewardFunction();

            double topp = calc.calculateTOPP(battery, buffer, popularity, tieStrength, density);
            this.lastTOPP = topp;
            double reward = rewarder.computeGradedReward(true, 500);
            ContextState sensed = env.senseContext(battery, buffer, popularity, tieStrength);

            String action = agent.chooseAction(sensed);

            System.out.println("[RLRouter] Battery = " + battery + " (" + current + " / " + max + " J)");
            System.out.println("[RLRouter] Buffer  = " + buffer + " (" + usedBuffer + " / " + totalBuffer + " bytes)");
            System.out.println("[RLRouter] Popularity = " + popularity);
            System.out.println("[RLRouter] Tie Strength to Node " + neighborId + " = " + tieStrength);
            System.out.println("[RLRouter] Chosen Action: " + action);

            // Passive Q-table update to ensure it's populated
            agent.update(state, action, 0.0, state);
            System.out.println("[DEBUG] Passive Q-update for: " + state + ", action: " + action);


        }
    }

    private void logQTableIfNeeded() {
        if (enableQTableReport && ((int) SimClock.getTime()) % qtableDumpInterval == 0) {
            QTableReport.saveQTableToFile(agent.getQTable(), String.valueOf(getHost().getAddress()));
        }
    }

    private void checkDeliveredMessages() {
        for (Message msg : new ArrayList<>(getMessageCollection())) {
            if (msg.getTo().equals(getHost())) {
                int copies = copyControl.getCopies(msg.getId());
                System.out.println("[L_COPY] Message " + msg.getId() + " delivered with " + copies + " copies");

                // Laporan salinan
                CopyMonitor.getInstance().messageDelivered(msg.getId());
                if (CopySummaryReport.reportInstance != null) {
                    CopySummaryReport.reportInstance.messageDelivered(msg.getId());
                }

                copyControl.remove(msg.getId());
                deleteMessage(msg.getId(), false);
                agent.update(lastState, "FORWARD", new RewardFunction().computeBinaryReward(true), lastState);

            }
        }
    }



    @Override
    public Message messageTransferred(String id, DTNHost from) {
        super.messageTransferred(id, from);
        int totalNodes = allHosts != null ? allHosts.size() : 1;
        int L_cur = copyControl.getCopies(id);
        int maxL = 5;

        Set<Integer> tensI = new HashSet<>(uniqueContacts);
        Set<Integer> tensJ = new HashSet<>();
        if (from.getRouter() instanceof RLRouter) {
            RLRouter senderRouter = (RLRouter) from.getRouter();
            tensJ = senderRouter.uniqueContacts;
        }

        int adaptiveLimit = copyControl.calculateAdaptiveLimit(L_cur, maxL, tensI, tensJ, totalNodes);

        double myUtility = this.lastTOPP;
        double peerUtility = (from.getRouter() instanceof RLRouter) ? ((RLRouter) from.getRouter()).lastTOPP : 0.0;
        double myQ = agent.getQValue(lastState, "FORWARD");
        double peerQ = 0.0;
        if (from.getRouter() instanceof RLRouter) {
            RLRouter peerRouter = (RLRouter) from.getRouter();
            ContextState peerState = peerRouter.lastState;
            peerQ = peerState != null ? peerRouter.agent.getQValue(peerState, "FORWARD") : 0.0;
        }

        boolean isDest = from.getAddress() == getHost().getAddress();
        boolean isQualified = copyControl.canForward(id, myUtility, peerUtility, myQ, peerQ, isDest);

        System.out.println("[DEBUG] Copy Decision for msg " + id + ": isQualified = " + isQualified);
        System.out.println("[CopyEval] Message " + id + ": MyU=" + myUtility + ", PeerU=" + peerUtility +
                ", MyQ=" + myQ + ", PeerQ=" + peerQ + ", isQualified = " + isQualified);

        if (!isQualified) {
            System.out.println("[CopyControl] Dropped: peer not qualified for message " + id);
            System.out.println("[CopyRejected] Message " + id + " not forwarded to " + from.getAddress());

            return null;

        }

        if (L_cur <= 1) {
            copyControl.forwardFinal(id);
        } else {
            copyControl.forwardHalf(id);
            int remainingCopies = copyControl.getCopies(id);
            Message msg = getMessage(id);
            if (msg != null) {
                msg.updateProperty("copiesLeft", remainingCopies);
            }

        }

        // 🧠 Tambahkan info copiesLeft ke message yang akan dikirim
        int remainingCopies = copyControl.getCopies(id);
        Message msg = getMessage(id);
        if (msg != null) {
            msg.updateProperty("copiesLeft", remainingCopies);
        }

        // 📄 Laporan
        if (CopyActionLogReport.instance != null) {
            CopyActionLogReport.instance.logCopy(id, getHost().getAddress(), from.getAddress(), myUtility, myQ, SimClock.getTime());
        }

        if (CopySummaryReport.reportInstance != null) {
            CopySummaryReport.reportInstance.messageCopied(id);
        }

        // 🔁 Sinkronisasi Q-table
        if (from.getRouter() instanceof RLRouter) {
            RLRouter senderRouter = (RLRouter) from.getRouter();
            QLearning senderAgent = senderRouter.getQLearning();
            this.agent.syncWith(senderAgent.getQTable());
            senderAgent.syncWith(this.agent.getQTable());
            System.out.println("[SYNC] Q-table synced between node " + from.getAddress() + " and " + getHost().getAddress());
        }

        // 🌱 RL Update
        if (!isDead && lastState != null) {
            double delay = SimClock.getTime() - msg.getCreationTime();
            double reward = new RewardFunction().computeGradedReward(true, delay);

            ContextState nextState = lastState;
            agent.update(lastState, "FORWARD", reward, nextState);
            agent.logQTable(getHost().getAddress());
            if (energyManager != null && msg != null) {
                double recvCost = computeReceiveCost(msg);
                if (energyManager.canAct(recvCost)) {
                    energyManager.consume(recvCost);
                } else {
                    System.out.println("[BATTERY] Node " + getHost().getAddress() + " kehabisan baterai saat menerima message " + msg.getId());
                    isDead = true;
                }
            }


        }

        return msg;
    }






    @Override
    public boolean requestDeliverableMessages(Connection con) {
        boolean can = !isDead && super.requestDeliverableMessages(con);
        //System.out.println("[DEBUG] requestDeliverableMessages from node " + getHost().getAddress() + ": " + can);
        return can;
    }


    @Override
    public int receiveMessage(Message m, DTNHost from) {
        if (!isDead) {
            Object c = m.getProperty("copiesLeft");
            if (c != null) {
                System.out.println("[RECEIVED] Message " + m.getId() + " with copiesLeft = " + c);
            }
        }

        return isDead ? DENIED_OLD : super.receiveMessage(m, from);
    }


    @Override
    public boolean createNewMessage(Message m) {
        CopyMonitor.getInstance().registerMessage(m.getId());
        boolean created = !isDead && super.createNewMessage(m);
        if (created && CopySummaryReport.reportInstance != null) {
            CopySummaryReport.reportInstance.messageCreated(m.getId());
        }
        return created;
    }


    @Override
    protected boolean canStartTransfer() {
        System.out.println("[DEBUG] canStartTransfer() called for node " + getHost().getAddress());
        return !isDead && super.canStartTransfer();
    }
    public QLearning getQLearning() {
        return this.agent;
    }






}
