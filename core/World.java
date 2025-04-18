package core;

import input.EventQueue;
import input.ExternalEvent;
import input.ScheduledUpdatesQueue;
import interfaces.ConnectivityGrid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class World {
	public static final String SETTINGS_NS = "Optimization";
	public static final String CELL_SIZE_MULT_S = "cellSizeMult";
	public static final String RANDOMIZE_UPDATES_S = "randomizeUpdateOrder";
	public static final int DEF_CON_CELL_SIZE_MULT = 5;
	public static final boolean DEF_RANDOMIZE_UPDATES = true;

	private int sizeX;
	private int sizeY;
	private List<EventQueue> eventQueues;
	private double updateInterval;
	private SimClock simClock;
	private double nextQueueEventTime;
	private EventQueue nextEventQueue;
	private List<DTNHost> hosts;
	private boolean simulateConnections;
	private ArrayList<DTNHost> updateOrder;
	private boolean isCancelled;
	private List<UpdateListener> updateListeners;
	private ScheduledUpdatesQueue scheduledUpdates;
	private int conCellSizeMult;

	public World(List<DTNHost> hosts, int sizeX, int sizeY, double updateInterval,
				 List<UpdateListener> updateListeners, boolean simulateConnections,
				 List<EventQueue> eventQueues) {
		this.hosts = hosts;
		this.sizeX = sizeX;
		this.sizeY = sizeY;
		this.updateInterval = updateInterval;
		this.updateListeners = updateListeners;
		this.simulateConnections = simulateConnections;
		this.eventQueues = eventQueues;

		this.simClock = SimClock.getInstance();
		this.scheduledUpdates = new ScheduledUpdatesQueue();
		this.isCancelled = false;

		setNextEventQueue();
		initSettings();
	}

	private void initSettings() {
		Settings s = new Settings(SETTINGS_NS);
		boolean randomizeUpdates = DEF_RANDOMIZE_UPDATES;

		if (s.contains(RANDOMIZE_UPDATES_S)) {
			randomizeUpdates = s.getBoolean(RANDOMIZE_UPDATES_S);
		}
		this.updateOrder = randomizeUpdates ? new ArrayList<>(this.hosts) : null;

		if (s.contains(CELL_SIZE_MULT_S)) {
			conCellSizeMult = s.getInt(CELL_SIZE_MULT_S);
		} else {
			conCellSizeMult = DEF_CON_CELL_SIZE_MULT;
		}

		if (conCellSizeMult < 2) {
			throw new SettingsError("Too small value (" + conCellSizeMult + ") for " + SETTINGS_NS + "." + CELL_SIZE_MULT_S);
		}
	}

	public void warmupMovementModel(double time) {
		if (time <= 0) return;

		while (SimClock.getTime() < -updateInterval) {
			moveHosts(updateInterval);
			simClock.advance(updateInterval);
		}
		double finalStep = -SimClock.getTime();
		moveHosts(finalStep);
		simClock.setTime(0);
	}

	public void setNextEventQueue() {
		EventQueue nextQueue = scheduledUpdates;
		double earliest = nextQueue.nextEventsTime();

		for (EventQueue eq : eventQueues) {
			if (eq.nextEventsTime() < earliest) {
				nextQueue = eq;
				earliest = eq.nextEventsTime();
			}
		}

		this.nextEventQueue = nextQueue;
		this.nextQueueEventTime = earliest;
	}

	public void update() {
		double runUntil = SimClock.getTime() + this.updateInterval;
		setNextEventQueue();

		while (this.nextQueueEventTime <= runUntil) {
			simClock.setTime(this.nextQueueEventTime);
			ExternalEvent ee = this.nextEventQueue.nextEvent();
			ee.processEvent(this);
			updateHosts();
			setNextEventQueue();
		}

		moveHosts(this.updateInterval);
		simClock.setTime(runUntil);
		updateHosts();

		for (UpdateListener ul : this.updateListeners) {
			ul.updated(this.hosts);
		}
	}

	private void updateHosts() {
		if (this.updateOrder == null) {
			for (int i = 0; i < hosts.size(); i++) {
				if (this.isCancelled) break;
				DTNHost host = hosts.get(i);
				if (host == null) {
					System.err.println("[ERROR] Host index " + i + " is null in updateHosts()");
					continue;
				}
				host.update(simulateConnections);
			}
		} else {
			Random rng = new Random(SimClock.getIntTime());
			Collections.shuffle(this.updateOrder, rng);
			for (int i = 0; i < updateOrder.size(); i++) {
				if (this.isCancelled) break;
				DTNHost host = updateOrder.get(i);
				if (host == null) {
					System.err.println("[ERROR] updateOrder Host index " + i + " is null");
					continue;
				}
				host.update(simulateConnections);
			}
		}
	}

	private void moveHosts(double timeIncrement) {
		for (int i = 0; i < hosts.size(); i++) {
			DTNHost host = hosts.get(i);
			if (host == null) {
				System.err.println("[ERROR] Host index " + i + " is null in moveHosts()");
				continue;
			}
			host.move(timeIncrement);
		}
	}

	public void cancelSim() {
		this.isCancelled = true;
	}

	public List<DTNHost> getHosts() {
		return this.hosts;
	}

	public int getSizeX() {
		return this.sizeX;
	}

	public int getSizeY() {
		return this.sizeY;
	}

	public DTNHost getNodeByAddress(int address) {
		if (address < 0 || address >= hosts.size()) {
			throw new SimError("No host for address " + address + ". Valid range: 0-" + (hosts.size() - 1));
		}
		DTNHost node = this.hosts.get(address);
		if (node == null) {
			throw new SimError("Node at address " + address + " is null!");
		}
		return node;
	}

	public void scheduleUpdate(double simTime) {
		scheduledUpdates.addUpdate(simTime);
	}
}
