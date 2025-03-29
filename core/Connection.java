package core;

import routing.MessageRouter;

/**
 * A connection between two DTN nodes.
 */
public abstract class Connection {
	protected DTNHost toNode;
	protected NetworkInterface toInterface;
	protected DTNHost fromNode;
	protected NetworkInterface fromInterface;
	protected DTNHost msgFromNode;

	private boolean isUp;
	protected Message msgOnFly;
	/** how many bytes this connection has transferred */
	protected int bytesTransferred;

	public Connection(DTNHost fromNode, NetworkInterface fromInterface,
					  DTNHost toNode, NetworkInterface toInterface) {
		this.fromNode = fromNode;
		this.fromInterface = fromInterface;
		this.toNode = toNode;
		this.toInterface = toInterface;
		this.isUp = true;
		this.bytesTransferred = 0;
	}

	public boolean isUp() {
		return this.isUp;
	}

	public boolean isInitiator(DTNHost node) {
		return node == this.fromNode;
	}

	public void setUpState(boolean state) {
		this.isUp = state;
	}

	public abstract int startTransfer(DTNHost from, Message m);

	public void update() {};

	public void abortTransfer() {
		assert msgOnFly != null : "No message to abort at " + msgFromNode;
		int bytesRemaining = getRemainingByteCount();
		this.bytesTransferred += msgOnFly.getSize() - bytesRemaining;
		getOtherNode(msgFromNode).messageAborted(this.msgOnFly.getId(),
				msgFromNode, bytesRemaining);
		clearMsgOnFly();
	}

	public abstract int getRemainingByteCount();

	protected void clearMsgOnFly() {
		this.msgOnFly = null;
		this.msgFromNode = null;
	}

	public void finalizeTransfer() {
		assert this.msgOnFly != null : "Nothing to finalize in " + this;
		assert msgFromNode != null : "msgFromNode is not set";
		this.bytesTransferred += msgOnFly.getSize();
		getOtherNode(msgFromNode).messageTransferred(this.msgOnFly.getId(),
				msgFromNode);
		clearMsgOnFly();
	}

	public abstract boolean isMessageTransferred();

	public boolean isReadyForTransfer() {
		return this.isUp && this.msgOnFly == null;
	}

	public Message getMessage() {
		return this.msgOnFly;
	}

	public abstract double getSpeed();

	public int getTotalBytesTransferred() {
		if (this.msgOnFly == null) {
			return this.bytesTransferred;
		}
		else {
			if (isMessageTransferred()) {
				return this.bytesTransferred + this.msgOnFly.getSize();
			}
			else {
				return this.bytesTransferred +
						(msgOnFly.getSize() - getRemainingByteCount());
			}
		}
	}

	public DTNHost getOtherNode(DTNHost node) {
		if (node == this.fromNode) {
			return this.toNode;
		}
		else {
			return this.fromNode;
		}
	}

	public NetworkInterface getOtherInterface(NetworkInterface i) {
		if (i == this.fromInterface) {
			return this.toInterface;
		}
		else {
			return this.fromInterface;
		}
	}

	public NetworkInterface getInterface1() {
		return fromInterface;
	}

	public NetworkInterface getInterface2() {
		return toInterface;
	}

	public String toString() {
		return fromNode + "<->" + toNode + " (" + getSpeed() + "Bps) is " +
				(isUp() ? "up":"down") +
				(this.msgOnFly != null ? " transferring " + this.msgOnFly  +
						" from " + this.msgFromNode : "");
	}

	public void disconnect(NetworkInterface initiator) {
		setUpState(false);
		NetworkInterface other = initiator == fromInterface ? toInterface : fromInterface;
		initiator.disconnect(other);
		if (!fromInterface.removeConnection(this, initiator))
			throw new SimError("No connection " + this + " found in " + fromInterface);
		if (!toInterface.removeConnection(this, initiator))
			throw new SimError("No connection " + this + " found in " + toInterface);
		toNode.connectionDown(this);
		fromNode.connectionDown(this);
	}

	public void tearDown() {
		this.setUpState(false);
		this.getInterface1().getHost().connectionDown(this);
		this.getInterface2().getHost().connectionDown(this);
		this.getInterface1().getConnections().remove(this);
		this.getInterface2().getConnections().remove(this);
	}
}