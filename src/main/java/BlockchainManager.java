import java.io.*;
import java.net.*;
import java.util.*;

import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class BlockchainManager extends Thread {
		
	//HTTP response codes
	private static final String OK_HEADER = "HTTP/1.1 200 OK\r\n" +
		"Content-Type: text/html\r\n" + 
		"Content-Length: ";
	private static final String OK_HEADER_END = "\r\n\r\n";
	private static final String NOT_FOUND_HEADER = "HTTP/1.1 404 Not Found\r\n";
	private static final String NO_CONTENT_HEADER = "HTTP/1.1 204 No Content\r\n";
	private static final String BAD_REQUEST_HEADER = "HTTP/1.1 400 Bad Request\r\n";
	

	//address for finding peers and initial ports for sockets
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
	
	

	//socket for finding and peers
	private MulticastSocket udpSocket;
	//socket for exchanging chains and blocks with other instances
	private ServerSocket chainServerSocket;
	//socket for responded to user requests from web browser
	private ServerSocket webServerSocket;
	
	//used to find and respond to peers to create peer list
	private PeerResponder peerResponder;
	//used to receive blocks and chains from other instances
	private Receiver receiver;
	//used for parsing received strings into json objects
	private JSONParser parser;
	
	
	public BlockchainManager() {
		peerList = new ArrayList<InetSocketAddress>();
		parser = new JSONParser();
		chainBindPort = CHAIN_BASE_PORT;
		webBindPort = WEB_BASE_PORT;
		
		//create new empty blockchain
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
			//create and bind udp multicast socket for finding peers, join multicast group
			udpSocket = new MulticastSocket(chainBindPort + UDP_OFFSET);
			udpSocket.setSoTimeout(1000);
			InetAddress groupAddress = InetAddress.getByName(MULTICAST_ADDRESS);
			udpSocket.joinGroup(groupAddress);
			
			//create udp multicast responder
			peerResponder = new PeerResponder(udpSocket, groupAddress);
		} catch (IOException ioex) {
			ioex.printStackTrace();
		}
		
		
		//bind web server socket, loop until a port is found to bind to
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
		
		//obtain the longest valid chain from the discovered peers
		Sender.findBestChainFromPeers(peerList);
		
		//start response system to update other instances' peer lists upon request
		System.out.println("Waiting for Peer Requests");
		peerResponder.start();
		
		//start receiving blocks and chain exchange requests
		receiver.start();

		try {
			System.out.println("\n\nHTTP server running on:\n"
			+  "   Local:  127.0.0.1:" + webBindPort + "\n"
			+  "   Remote: " + Inet4Address.getLocalHost().getHostAddress()
			+  ":" + webBindPort);
		} catch (IOException ioex) {
			ioex.printStackTrace();
		}

		//main loop for handling browser input from user
		while (true) {
			try {
				//connect to user over http
				Socket socket = webServerSocket.accept();
				InputStream inputStream = socket.getInputStream();
				BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

				//will contain the length of any data sent via POST method (if any)
				int postContentLength = 0;

				//read first line of user input containing http method (GET,POST, etc)
				String line = reader.readLine();
				String method = line;
				
				//loop over input until empty
				while (!line.isEmpty()) {
					//System.out.println(line);
					
					//if post method used, capture length of post content
					if(method.substring(0,4).equals("POST") && line.length() > 15) {
						if(line.substring(0,16).equals("Content-Length: ")) {
							postContentLength = Integer.valueOf(line.substring(16));
						}
					}

					//read next line of input
					line = reader.readLine();
				}
				
				//string will contain any data sent via POST method
				String postString = null;
				
				//if POST content is present, read and place it into postString
				if(postContentLength > 0) {
					char[] postData = new char[postContentLength];
					reader.read(postData, 0, postData.length);		
					postString = new String(postData);
				}
				
				//System.out.println("method is:" + method);
				//System.out.println("post data is: " + postString + "\n");
				
				//determine how to respond based on received method and POST data
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
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}
	
	//check if an address is in the peer list
	public static boolean addressInPeerList(InetSocketAddress address) {
		
		String addressString = address.getAddress().getHostAddress();
		int port = address.getPort();
		
		boolean found = false;
		
		//loop over peer list until end or found address
		int i = 0;
		while (i < peerList.size() && !found) {
			if (peerList.get(i).getAddress().getHostAddress().equals(addressString)
			&& peerList.get(i).getPort() == port) {

				found = true;
			}
			i++;
		}
		
		return found;
	}
	
	private static String performActionFromHeader(
	String method, String postString) throws IOException {
		
		//string which will be returned at the end, start by assuming NOT_FOUND
		String response = NOT_FOUND_HEADER;

		//split http method by spaces, store method and file requested
		String[] tokens = method.split("\\s+");
		String methodName = tokens[0];
		String fileName = tokens[1];
		
		//if file is longer than 1 char long, remove slash at beginning
		if (fileName.length() > 1) {
			fileName = fileName.substring(1);
		}
		
		//if a question mark is present in the file name, remove it and everything after
		if (fileName.indexOf('?') != -1) {
			fileName = fileName.substring(0, fileName.indexOf('?'));
		}
		
		try {
			//http GET method responses
			if (methodName.equals("GET")) {

				//if file requested is default page, return index file
				if (fileName.equals("/")) {
					String dataString = fileToString("index.html");
					response = OK_HEADER + dataString.length() + OK_HEADER_END + dataString;
					
				//if getlists command is called, return list of peers and blocks as html string
				} else if (fileName.equals("getlists")) {

					String peerListString = "All known peer addresses:<br>";
					
					//place all known peers into a list as a string
					for(int i = 0; i < peerList.size(); i ++) {
						peerListString += peerList.get(i).getAddress().getHostAddress()
							+ ":" + peerList.get(i).getPort() + "<br>";
					}

					//place all current blocks into list as string
					String blockListString = "<br><br>All blocks in current local list:<br><br>";
					blockListString += blockchain.toHtmlString();
					
					//combine peer and block lists and put into http response
					String dataString = peerListString + blockListString;
					response = OK_HEADER + dataString.length() + OK_HEADER_END + dataString;	
					
				//anything other than default or getlists is not valid for now
				} else {
					response = BAD_REQUEST_HEADER;				
				}
				
			//http HEAD method responses
			} else if (methodName.equals("HEAD")) {
				if (fileName.equals("/")) {
					String dataString = fileToString("index.html");
					response = OK_HEADER + dataString.length() + OK_HEADER_END;
				} else {
					response = BAD_REQUEST_HEADER;			
				}

			//http POST method responses
			} else if (methodName.equals("POST")) {
				
				if (postString != null) {
					//take post data from after equals sign
					if (postString.indexOf('=') != -1) {
						postString = postString.substring(postString.indexOf('=') + 1);
					}
					
					//decode from html url format
					postString = URLDecoder.decode(postString, "UTF-8");
				
					//if addvalid command applied, create a valid block from the
					//post string and broadcast the block to other instances
					if (fileName.equals("addvalid")) {
						
						addBlock("valid", postString);
						
						String dataString = "Mining Complete. Chain Modified";
						response = OK_HEADER + dataString.length() + OK_HEADER_END + dataString;
						
					//if addlocal command applied, create a valid block from the
					//post string but do not broadcast the block
					} else if (fileName.equals("addlocal")){
						
						addBlock("local", postString);
						
						String dataString = "Mining Complete. Added local valid block with data: " + postString;
						response = OK_HEADER + dataString.length() + OK_HEADER_END + dataString;
						
					//if addinvalid command applied, create an invalid block from the
					//post string and add to local chain, do not broadcast
					} else if (fileName.equals("addinvalid")){
						
						addBlock("invalid", postString);
						
						String dataString = "Added local invalid block with data: " + postString;
						response = OK_HEADER + dataString.length() + OK_HEADER_END + dataString;
						
					//anything other than addvalid, addlocal, or addinvalid is not valid for now
					} else {
						response = BAD_REQUEST_HEADER;
					}
					
				//null post string with post method is not valid
				} else {
					response = BAD_REQUEST_HEADER;
				}	
				
			//anything other than GET, HEAD, or POST is not valid for now
			} else {
				response = BAD_REQUEST_HEADER;
			}
		} catch (FileNotFoundException fnfex) {
			response = NOT_FOUND_HEADER;
		}
		
		return response;
	}
	
	private static void addBlock(String type, String data) {
		
		//create a valid block from data and broadcast the block to other instances
		if (type.equals("valid")) {

			Block toAdd = null;
			
			//create, mine and broadcast block until accepted by peers
			//rejection by peers will modify local chain
			do {		
				System.out.println("Creating block with data: " + data);
			
				toAdd = blockchain.getBlockFromData(data);
			
				System.out.println("Block created.");
				System.out.println("Mining Block...");
				
				//turn block into valid block
				toAdd.mineBlock();
				
				System.out.println("Mining finished.");
				
				//add block to local chain, will be modified if rejected by peers
				if(blockchain.addBlock(toAdd)) {
					System.out.println("Block successfully added to chain.");
				} else {
					System.out.println("ERROR: Block not added to chain.");
				}
				
			//broadcast block to peers, repeat until accepted
			} while (!Sender.broadcastBlock(toAdd, peerList));
			
			System.out.println("Broadcast succeeded");

		//create a valid block from data, do not broadcast
		} else if (type.equals("local")) {

			System.out.println("Creating local block with data: " + data);
			
			Block toAdd = blockchain.getBlockFromData(data);
			
			System.out.println("Block created.");
			System.out.println("Mining Block...");
			
			//turn block into valid block
			toAdd.mineBlock();
			
			System.out.println("Mining finished.");
			System.out.println("Adding block to local chain...");
			
			if(blockchain.addBlock(toAdd)) {
				System.out.println("Block successfully added to chain.");
			} else {
				System.out.println("ERROR: Block not added to chain.");
			}
			
		//create an invalid block from data, do not broadcast
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
		
		//get file as a stream
		InputStream inputStream = 
			BlockchainManager.class.getClassLoader().getResourceAsStream(fileName);
			
		//buffer to store each byte read from file
		byte[] oneByteBuffer = new byte[1];

		//read input bytes into arraylist
		//when end of stream is hit, read will return -1
		while (inputStream.read(oneByteBuffer, 0, 1) != -1) {
			fileByteList.add(oneByteBuffer[0]);
		}

		//convert arraylist of bytes into primitive byte array
		byte[] fileBuffer = new byte[fileByteList.size()];	
		for (int i = 0; i < fileBuffer.length; i++) {
			fileBuffer[i] = fileByteList.get(i).byteValue();
		}

		if (inputStream != null) {
			inputStream.close();
		}
		
		//return byte array containing file as a string
		return new String(fileBuffer, "UTF-8");
	}
}