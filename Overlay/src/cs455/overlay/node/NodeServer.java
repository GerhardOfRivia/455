package cs455.overlay.node;

import java.text.*;
import java.util.*;
import java.io.*;
import java.net.*;

import cs455.overlay.msg.*;
import cs455.overlay.util.*;

/**
 * 
 * @author G van Andel
 *
 * @see MessagingNode.NodeMain
 * @see AbstractServer
 */

public class NodeServer extends AbstractServer {
	
	/**
	 * @see Dijkstra
	 */
	Dijkstra dijkstra;
	
	// CONSTRUCTOR -----------------------------------------------------
	
	/**
	 * Constructs a new server.
	 *
	 */
	public NodeServer() throws IOException {
		super(0);
	}
	// ACCESSING METHODS ------------------------------------------------
	
	/**
	 * When the registry sends the costs we also add the costs of the
	 * connections to the NodeConnections.
	 * @see NodeConnection
	 * @param nodes [ 127.0.0.0:40000 127.0.0.1:40001 4]
	 */
	public void setWeights(String[] nodes) {
		for (String info : nodes) {
			String[] host = info.split(" ");
			if (host[0].equals(this.getName()) == false)
				continue;
			String[] node = host[0].split(":");
			int port = super.validateInput(node[1]);
			NodeConnection nodeConnection = super.getConnection(host[1]);
			int cost = super.validateInput(host[2]);
			nodeConnection.setCost(cost);
			try {
				nodeConnection.sendToNode(new EdgeMessage(port, cost));
			} catch (IOException e) {
				System.err.println("Error when sending wieght information.");
			}
		}
		dijkstra.addOverlay(nodes);
	}
	
	/**
	 * Additional information containing all the nodes and the
	 * This is an array that should be formated as "host:port"
	 *
	 * @param nodes [ 129.82.44.175:38271 ]
	 */
	public void setInfo(String[] nodes) {
		for (String node : nodes) {
			String[] host = node.split(":");
			addConnection(host[0], host[1]);
		}
	} 
	
	/**
	 * Add a connection to the server to another server.
	 * @param host
	 * @param sPort
	 */
	public void addConnection(String host, String sPort) {
		int port = validateInput(sPort);
		if (port == 0)
			return;
		// Wait here for new connection attempts, or a timeout
		Socket clientSocket = null;
		try {
			clientSocket = new Socket(host, port);
		} catch (IOException e) {
			System.err.println("Could not connect to: "+host+":"+port);
			return;
		}
		NodeConnection node = null;
		synchronized (this) {
			try {
				node = new NodeConnection(this.nodeThreadGroup, clientSocket, this);
			} catch (IOException e) {}
		}
		String hostName = super.getTargetHostName(host);
		node.setClientInfo(hostName, host, port);
	}
	
	/**
	 * Receive all the connections from the server.
	 *  
	 * @param host [ ip:port ]
	 */
	public void makeDijkstra(String[] info) {
		String address = super.getHost();
		int port = super.getPort();
		dijkstra = new Dijkstra(info, address, port);
	}
	
	/**
	 * Start sending messages.
	 */
	public void startMessaging(int number) {
		for (int i = 0; i < number; i++) {
			String target = dijkstra.getRandomNode();
			String nextHop = dijkstra.getNextHop(target);
			sendTaskMessage(super.getConnection(nextHop), target);
		}
	}
	
	/**
	 * Test sending messages to host with 0 hops.
	 */
	public void testTask() {
		if (dijkstra == null) {
			System.err.println("Overlay has not been setup yet.");
			return;
		}
		System.out.println("TEST TASK.");
		String target = super.getConnectionNames()[0];
		sendTaskMessage(super.getConnection(target), target);
	}
	
	/**
	 * Test sending messages to any host in the overlay.
	 */
	public void testMessaging() {
		if (dijkstra == null) {
			System.err.println("Overlay has not been setup yet.");
			return;
		}
		System.out.println("TEST MESSAGING.");
		String target = dijkstra.getRandomNode();
		String nextHop = dijkstra.getNextHop(target);
		sendTaskMessage(super.getConnection(nextHop), target);

	}
	
	/**
	 * Send the task message to the {@link NodeConnection} for the 
	 * destination of target.
	 * @param node
	 * @param target
	 */
	private void sendTaskMessage(NodeConnection node, String target) {
		if (debug)
			System.out.println("Target: "+target+" node: "+node);
		if (node == null)
			System.err.println("sendTaskMessage: FIRE! FIRE!");
		Random rand = new Random();
		for (int count = 0; count < 5; count++) {
			int random = rand.nextInt();
			try {
				node.sendToNode(new TaskMessage(target, random));
				stats.addSent(random);
			} catch (IOException e) {
				System.out.println(e.toString());
				e.printStackTrace();
			}
		}
	}

	
	// HOOK METHODS -----------------------------------------------------
	/**
	 * TODO
	 * @return
	 */
	public String getNodeCost() {
		if (dijkstra == null)
			return "Overlay has not been setup yet.";
		String ret = "";
		String[] info = dijkstra.getDist();
		for (String i : info)
			ret += i + "\n";
		return ret;
	}
	
	/**
	 * TODO
	 * @return
	 */
	public String getShortestPath() {
		if (dijkstra == null)
			return "Overlay has not been setup yet.";
		String ret = "";
		String[] info = dijkstra.getPaths();
		for (String i : info)
			ret += i + "\n";
		return ret;
	}
	
	public void nodeConnected(NodeConnection nodeConnection) {
		DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
		Date date = new Date();
		System.out.println(nodeConnection+" connected at "+dateFormat.format(date));
	}


	synchronized public void nodeDisconnected(NodeConnection nodeConnection) {
		DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
		Date date = new Date();
		System.out.println(nodeConnection+" disconnected at "+dateFormat.format(date));
	}

	public void listeningException(Throwable exception) {
		System.out.println("listeningException :: "+exception.toString());
		System.exit(1);
	}

	public void serverStarted() {
		System.out.println("Node server started "+this.getName());
	}
	
	protected void serverClosed() {
		if (debug)
			System.out.println("serverClosed :: Exitting.");
	}
	
	/**
	 * TODO
	 */
	public void updateConnectionWeight(EdgeMessage m, NodeConnection client) {
		if (debug)
			System.out.println(m);
		String ipAddress = client.getAddress();
		String hostName = super.getTargetHostName(ipAddress);
		client.setClientInfo(hostName, ipAddress, m.getPort());
		client.setCost(m.getCost());
	}
	
	/**
	 * TODO
	 */
	public void checkMessage(TaskMessage m, NodeConnection client) {
		if (m.getDest().equals(getName()) == true) {
			stats.addReceived(m.getNumber());
			return;
		}
		String hop = dijkstra.getNextHop(m.getDest());
		try {
			super.getConnection(hop).sendToNode(m);
			stats.addRelayed();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	protected void MessageFromNode(Object msg, NodeConnection client) {
		if (msg instanceof ProtocolMessage == false)
			return;
		ProtocolMessage m = (ProtocolMessage) msg;
		switch(m.getStringType()) {
			case "SINGLE_WEIGHT":
				updateConnectionWeight(m.convertToEdgeInformation(), client);
				break;
			case "TASK_MESSAGE":
				checkMessage(m.convertToTask(), client);
				break;
		}
	}
	
}
// End of AbstractServer Class
