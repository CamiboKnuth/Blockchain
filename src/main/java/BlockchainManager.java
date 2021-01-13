import java.io.*;
import java.net.*;
import java.util.*;

//import java.sql.*;

import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class BlockchainManager extends Thread {
	
	public static final String MULTICAST_ADDRESS = "233.3.3.3";
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
	
	private JSONParser parser;
	
	public BlockchainManager() {
		peerList = new ArrayList<InetSocketAddress>();
		parser = new JSONParser();
		bindPort = CHAIN_BASE_PORT;
		
		//TODO: instantiate blockchain from database
		blockchain = new Blockchain();
		
		//TODO: don't let bind attempts surpass UDP_OFFSET
		
		//bind blockchain server socket, loop until a port is found to bind to
		boolean bound = false;
		
		try {
			while(!bound) {
				try {
					chainServerSocket = new ServerSocket(bindPort);
					bound = true;
				} catch (BindException bex) {
					System.out.println("NOTE: CANNOT BIND ON PORT: " + bindPort);
					bindPort ++;
				}
			}
		
		} catch (IOException ioex) {
			ioex.printStackTrace();
		}
		
		//used to receive blocks and chain requests
		receiver = new Receiver(chainServerSocket);
		
		try {
			//create and bind udp multicast socket, join multicast group
			udpSocket = new MulticastSocket(bindPort + UDP_OFFSET);
			udpSocket.setSoTimeout(1000);
			InetAddress groupAddress = InetAddress.getByName(MULTICAST_ADDRESS);
			udpSocket.joinGroup(groupAddress);
			
			
			//create udp multicast responder
			peerResponder = new PeerResponder(udpSocket, groupAddress);
		} catch (IOException ioex) {
			ioex.printStackTrace();
		}
	}
	
	public void run() {
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		System.out.println("Bound to port: " + bindPort);
		
		try {
			//request list of peers from blockchain network
			System.out.println("Finding Peers...");
			peerResponder.obtainPeerList(peerList);
			System.out.println("Peer Finding Complete.");
		
		} catch (IOException ioex) {
			ioex.printStackTrace();
		}
		
		Sender.findBestChainFromPeers(peerList);
		
		//start response system to update others peer lists upon request
		System.out.println("Waiting for Peer Requests");
		peerResponder.start();
		
		//start receiving blocks and chain exchange requests
		receiver.start();
		
		//main loop to handle user input
		while(true) {
			
			//	Commands :
			//		showchain
			//		showpeers
			//		addblock [data]
			//		addblockhere [data]
			//		chainex [address] [port]

			try {
			
				String input = reader.readLine();
				
				String[] arguments = input.split("\\s+");
				
				if (arguments[0].equals("showchain") && arguments.length == 1) {

					System.out.println("Chain is now:");
					blockchain.printChain();	
					
				} else if (arguments[0].equals("showpeers") && arguments.length == 1) {
					
					System.out.println("All known peers:");
					
					for(int i = 0; i < peerList.size(); i ++) {
						System.out.println("   " + peerList.get(i).getAddress().getHostAddress()
							+ ":" + peerList.get(i).getPort());
					}					

				} else if (arguments[0].equals("addblock") && arguments.length == 2) {
					
					System.out.println("Creating block with data: " + arguments[1]);

					Block toAdd = blockchain.getBlockFromData(arguments[1]);
					
					System.out.println("Block created.");
					System.out.println("Mining Block...");
					
					toAdd.mineBlock();
					
					System.out.println("Mining finished.");
					System.out.println("Attempting broadcast...");
					
					while (!Sender.broadcastBlock(toAdd, peerList)) {
						System.out.println("Broadcast failed.");
						
						toAdd = blockchain.getBlockFromData(arguments[1]);
					
						System.out.println("Block created.");
						System.out.println("Mining Block...");
						
						toAdd.mineBlock();
						
						System.out.println("Mining finished.");
						System.out.println("Attempting broadcast...");
					}
					
					System.out.println("Broadcast succeeded");
					System.out.println("Adding block to chain...");
					
					if(blockchain.addBlock(toAdd)) {
						System.out.println("Block successfully added to chain.");
					} else {
						System.out.println("ERROR: Block not added to chain.");
					}
					
				} else if (arguments[0].equals("addblockhere") && arguments.length == 2) {
					
					System.out.println("Creating local block with data: " + arguments[1]);
					
					Block toAdd = blockchain.getBlockFromData(arguments[1]);
					
					System.out.println("Block created.");
					System.out.println("Mining Block...");
					
					toAdd.mineBlock();
					
					System.out.println("Mining finished.");

					System.out.println("Adding block to local chain...");
					
					if(blockchain.addBlock(toAdd)) {
						System.out.println("Block successfully added to chain.");
					} else {
						System.out.println("ERROR: Block not added to chain.");
					}
					
				} else if (arguments[0].equals("chainex") && arguments.length == 3) {
					
					System.out.println("Sending exchange request to: " + arguments[1] + ":" + arguments[2]);
					
					Sender.sendChainExchange(
						new InetSocketAddress(
							InetAddress.getByName(arguments[1]),
							Integer.valueOf(arguments[2])
						)
					);

				} else {
					System.out.println("INVALID COMMAND");
				}
			
			} catch (IOException ioex) {
				ioex.printStackTrace();
			}
			
		
		}
	}
}