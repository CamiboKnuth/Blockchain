import java.net.*;

import java.sql.*;

import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class BlockchainManager extends Thread {
	
	public static final int CHAIN_BASE_PORT = 7770;
	public static final int UDP_OFFSET = 8;
	
	//stores all known addresses from blockchain network
	public static ArrayList<InetSocketAddress> peerList;
	
	//stores the list of blocks
	public static Blockchain blockchain;
	
	//port to which this instance's tcp server socket is bound
	public static int bindPort;
	
	
	private MulticastSocket udpSocket;
	private ServerSocket chainServerSocket;
	
	
	private PeerResponder peerResponder;
	private Receiver receiver;
	private Sender sender;
	
	private JSONParser parser;
	
	public BlockchainManager() {
		peerList = new ArrayList<InetSocketAddress>();
		parser = new JSONParser();
		bindPort = CHAIN_BASE_PORT;
		
		//bind blockchain server socket, loop until a port is found to bind to
		boolean bound = false;
		while(!bound) {
			try {
				chainServerSocket = new ServerSocket(bindPort);
				bound = true;
			} catch (BindException bex) {
				System.out.println("NOTE: CANNOT BIND ON PORT: " + bindPort);
				bindPort ++;
			}
		}
		
		//create and bind udp multicast socket, join multicast group
		udpSocket = new MulticastSocket(bindPort + BlockchainManager.UDP_OFFSET);
		udpSocket.setSoTimeout(1000);
		InetAddress groupAddress = InetAddress.getByName("228.5.6.7");
		udpSocket.joinGroup(groupAddress);
		
		//create tcp receiver and udp multicast responder
		receiver = new Receiver(chainServerSocket);
		peerResponder = new PeerResponder(udpSocket, groupAddress);
	}
	
	public void run() {
		//request list of peers from blockchain network
		peerResponder.obtainPeerList();
		
		//start response system to update others peer lists upon request
		peerResponder.start();
		
		//start receiving blocks and chain exchange requests
		receiver.start();
		
		while(true) {

		
		}
	}
}