import java.io.*;
import java.net.*;
import java.util.*;

//import java.sql.*;

import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class BlockchainManager extends Thread {
		
	private static final String OK_HEADER = "HTTP/1.1 200 OK\r\n" +
		"Content-Type: text/html\r\n" + 
		"Content-Length: ";
	private static final String OK_HEADER_END = "\r\n\r\n";
	private static final String NOT_FOUND_HEADER = "HTTP/1.1 404 Not Found\r\n";
	private static final String NO_CONTENT_HEADER = "HTTP/1.1 204 No Content\r\n";
	private static final String BAD_REQUEST_HEADER = "HTTP/1.1 400 Bad Request\r\n";
	


	public static final String MULTICAST_ADDRESS = "233.3.3.3";
	public static final int WEB_BASE_PORT = 8080;
	public static final int CHAIN_BASE_PORT = 7770;
	public static final int UDP_OFFSET = 10;
	
	//stores all known addresses from blockchain network
	public static ArrayList<InetSocketAddress> peerList;
	
	//stores the list of blocks
	public static Blockchain blockchain;
	
	//ports to which this instance's server sockets are bound
	public static int chainBindPort;
	public static int webBindPort;
	
	

	
	private MulticastSocket udpSocket;
	private ServerSocket chainServerSocket;
	private ServerSocket webServerSocket;
	
	private PeerResponder peerResponder;
	private Receiver receiver;
	
	private JSONParser parser;
	
	public BlockchainManager() {
		peerList = new ArrayList<InetSocketAddress>();
		parser = new JSONParser();
		chainBindPort = CHAIN_BASE_PORT;
		webBindPort = WEB_BASE_PORT;
		
		blockchain = new Blockchain();
		
		//bind blockchain server socket, loop until a port is found to bind to
		boolean bound = false;
		
		try {
			while(!bound) {
				try {
					chainServerSocket = new ServerSocket(chainBindPort);
					bound = true;
				} catch (BindException bex) {
					System.out.println("NOTE: CANNOT BIND ON PORT: " + chainBindPort);
					chainBindPort ++;
				}
			}
		} catch (IOException ioex) {
			ioex.printStackTrace();
		}
		
		//used to receive blocks and chain requests
		receiver = new Receiver(chainServerSocket);
		
		try {
			//create and bind udp multicast socket, join multicast group
			udpSocket = new MulticastSocket(chainBindPort + UDP_OFFSET);
			udpSocket.setSoTimeout(1000);
			InetAddress groupAddress = InetAddress.getByName(MULTICAST_ADDRESS);
			udpSocket.joinGroup(groupAddress);
			
			//create udp multicast responder
			peerResponder = new PeerResponder(udpSocket, groupAddress);
		} catch (IOException ioex) {
			ioex.printStackTrace();
		}
		
		
		bound = false;
		
		try {
			while(!bound) {
				try {
					webServerSocket = new ServerSocket(webBindPort);
					bound = true;
				} catch (BindException bex) {
					System.out.println("NOTE: CANNOT BIND ON PORT: " + webBindPort);
					webBindPort ++;
				}
			}
		} catch (IOException ioex) {
			ioex.printStackTrace();
		}		
	}
	
	public void run() {
		
		//BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		System.out.println("Blockchain Server Bound to Port: " + chainBindPort);
		System.out.println("HTTP Server Bound to Port: " + webBindPort);
		
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

		while (true) {
			
			try {
				//connect to user over http
				Socket socket = webServerSocket.accept();

				InputStream inputStream = socket.getInputStream();
				BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

				int postContentLength = 0;

				//read first line of user input containing http method (GET,POST, etc)
				String line = reader.readLine();
				String method = line;
				
				//loop over input
				while (!line.isEmpty()) {
					System.out.println(line);
					
					//if post method used, capture length of post content
					if(method.substring(0,4).equals("POST") && line.length() > 15) {
						if(line.substring(0,16).equals("Content-Length: ")) {
							postContentLength = Integer.valueOf(line.substring(16));
						}
					}

					line = reader.readLine();
				}
				
				String postString = null;
				
				//if post content is present, place it into postString
				if(postContentLength > 0) {
					char[] postData = new char[postContentLength];
					reader.read(postData, 0, postData.length);		
					postString = new String(postData);
				}
				
				System.out.println("method is:" + method);
				System.out.println("post data is: " + postString + "\n");
				
				//determine how to respond based on received method
				String response = performActionFromHeader(method, postString);
				
				//send response to user
				BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
					new BufferedOutputStream(socket.getOutputStream()), "UTF-8")
				);
				out.write(response);
				
				
				//close connection to user
				out.flush();
				out.close();
				socket.close();
				
			} catch (IOException ioex) {
				ioex.printStackTrace();
			}
		}
	}
	
	private static String performActionFromHeader(
		String method, String postString) throws IOException {
		
		String response = NOT_FOUND_HEADER;

		String[] tokens = method.split("\\s+");
		String methodName = tokens[0];
		String fileName = tokens[1];
		
		if (fileName.length() > 1) {
			fileName = fileName.substring(1);
		}
		
		if (fileName.indexOf('?') != -1) {
			fileName = fileName.substring(0, fileName.indexOf('?'));
		}
		
		try {
			if (methodName.equals("GET")) {
				if (fileName.equals("/")) {
					String dataString = fileToString("index.html");
					response = OK_HEADER + dataString.length() + OK_HEADER_END + dataString;
				} else if (fileName.equals("getlists")) {

					String peerListString = "All known peer addresses:<br>";
					
					for(int i = 0; i < peerList.size(); i ++) {
						peerListString += peerList.get(i).getAddress().getHostAddress()
							+ ":" + peerList.get(i).getPort() + "<br>";
					}

					String blockListString = "<br><br>All blocks in current local list:<br><br>";
					blockListString += blockchain.toHtmlString();
					
					String dataString = peerListString + blockListString;
					
					response = OK_HEADER + dataString.length() + OK_HEADER_END + dataString;	
					
				} else {
					response = BAD_REQUEST_HEADER;				
				}
			} else if (methodName.equals("HEAD")) {
				if (fileName.equals("/")) {
					String dataString = fileToString("index.html");
					response = OK_HEADER + dataString.length() + OK_HEADER_END;
				} else {
					response = BAD_REQUEST_HEADER;			
				}			
			} else if (methodName.equals("POST")) {
				
				if (postString != null) {
					if (postString.indexOf('=') != -1) {
						postString = postString.substring(postString.indexOf('=') + 1);
					}
					
					postString = URLDecoder.decode(postString, "UTF-8");
				
					if (fileName.equals("addvalid")) {
						
						addBlock("valid", postString);
						
						String dataString = "Mining Complete. Added shared valid block with data: " + postString;
						response = OK_HEADER + dataString.length() + OK_HEADER_END + dataString;
						
					} else if (fileName.equals("addlocal")){
						
						addBlock("local", postString);
						
						String dataString = "Mining Complete. Added local valid block with data: " + postString;
						response = OK_HEADER + dataString.length() + OK_HEADER_END + dataString;
						
					} else if (fileName.equals("addinvalid")){
						
						addBlock("invalid", postString);
						
						String dataString = "Added local invalid block with data: " + postString;
						response = OK_HEADER + dataString.length() + OK_HEADER_END + dataString;
						
					} else {
						response = BAD_REQUEST_HEADER;
					}
				} else {
					response = BAD_REQUEST_HEADER;
				}
			} else {
				response = BAD_REQUEST_HEADER;
			}
		} catch (FileNotFoundException fnfex) {
			response = NOT_FOUND_HEADER;
		}
		
		return response;
	}
	
	private static void addBlock(String type, String data) {
		if (type.equals("valid")) {

			boolean done = false;
			
			do {
				Block toAdd = null;
				
				do {		
					System.out.println("Creating block with data: " + data);
				
					toAdd = blockchain.getBlockFromData(data);
				
					System.out.println("Block created.");
					System.out.println("Mining Block...");
					
					toAdd.mineBlock();
					
					System.out.println("Mining finished.");
					System.out.println("Attempting broadcast...");
					
				} while (!Sender.broadcastBlock(toAdd, peerList));
				
				System.out.println("Broadcast succeeded");
				System.out.println("Adding block to chain...");
				
				if(blockchain.addBlock(toAdd)) {
					System.out.println("Block successfully added to chain.");
					done = true;
				} else {
					System.out.println("ERROR: Block not added to chain.");
				}

			} while (!done);			

		} else if (type.equals("local")) {

			System.out.println("Creating local block with data: " + data);
			
			Block toAdd = blockchain.getBlockFromData(data);
			
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
			
		} else if (type.equals("invalid")) {
		
			System.out.println("Creating local block with data: " + data);
			
			Block toAdd = blockchain.getBlockFromData(data);
			
			System.out.println("Invalid Block Created.");
			System.out.println("Adding invalid block to local chain...");
			
			blockchain.addInvalidBlock(toAdd);
			
			System.out.println("Invalid Block Added.");
		}
	}
	
	private static String fileToString(String fileName) throws IOException {		
		ArrayList<Byte> fileByteList = new ArrayList<Byte>();
		
		InputStream inputStream = 
			BlockchainManager.class.getClassLoader().getResourceAsStream(fileName);
			

		byte[] oneByteBuffer = new byte[1];

		//when end of stream is hit, read will return -1
		while (inputStream.read(oneByteBuffer, 0, 1) != -1) {
			fileByteList.add(oneByteBuffer[0]);
		}

		byte[] fileBuffer = new byte[fileByteList.size()];
		
		for (int i = 0; i < fileBuffer.length; i++) {
			fileBuffer[i] = fileByteList.get(i).byteValue();
		}

		if (inputStream != null) {
			inputStream.close();
		}
		
		return new String(fileBuffer, "UTF-8");
	}
}