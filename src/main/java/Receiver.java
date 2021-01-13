import java.io.*;
import java.net.*;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class Receiver extends Thread {
	
	private ServerSocket serverSocket;
	private static JSONParser parser;
	
	
	private volatile int receiverFlag;
	
	private final int DEFAULT_FLAG = 0;
	private final int CLOSE_FLAG = 1;
	
	public Receiver(ServerSocket serverSocket) {
		this.serverSocket = serverSocket;
		parser = new JSONParser();
	}
	
	public static void receiveChain(DataInputStream inputStream, DataOutputStream outputStream)
	throws IOException {
		
		//create new chain to compare to current chain
		Blockchain nextChain = new Blockchain();
		
		boolean done = false;
		
		//accept first input from other user
		String response = inputStream.readUTF();
		
		while (!done) {
			if (!response.equals("DONE")) {
				
				try {
					//parse block string into json object
					JSONObject object = (JSONObject) parser.parse(response);
					
					//handle if block, send REJ if not
					if(object.get("type").equals("block")) {
						
						
						//create block from sent json object
						Block nextBlock = new Block(object);
						
						
						System.out.println("Received block: "
							+ nextBlock.getNum() + ":" + nextBlock.getData());
						
						
						//add block to chain, send REJ if fail
						if (nextChain.addBlock(nextBlock)) {
							
							System.out.println("Block accepted, sending ACC");
							
							outputStream.writeUTF("ACC");
						} else {
							
							
							System.out.println("Block rejected, sending REJ");
							
							outputStream.writeUTF("REJ");
							done = true;
						}
						
					} else {
						outputStream.writeUTF("REJ");
						done = true;
					}
					
					//receive next input from other user
					response = inputStream.readUTF();
					
				} catch (ParseException parex) {
					done = true;
				}

			} else {
				done = true;
			}
		}
		
		//Compare chains and keep more valid chain
		
		System.out.println("Comparing chains...");
		
		if (BlockchainManager.blockchain.size() < nextChain.size()) {
			
			System.out.println("They're chain longer, replacing...");
			
			
			//they're more valid, replace chain
			BlockchainManager.blockchain = nextChain;
			
		} else if (BlockchainManager.blockchain.size() == nextChain.size()) {
			
			System.out.println("Chains equal size, checking timestamps");

			if (BlockchainManager.blockchain.getMostRecentTimestamp()
			> nextChain.getMostRecentTimestamp()) {
				
				System.out.println("They're more recent, replacing chain");

				//they're more valid, replace chain
				BlockchainManager.blockchain = nextChain;
			}	
		} 		
	}
	
	public void run() {
		
		//TODO: handle multiple connections?
		
		
		while(receiverFlag != CLOSE_FLAG) {
			
			try {
				
				System.out.println("Awaiting Connection...");
			
				//wait to receive connection
				Socket receiveSocket = serverSocket.accept();
				DataInputStream inputStream = new DataInputStream(receiveSocket.getInputStream());
				DataOutputStream outputStream = new DataOutputStream(receiveSocket.getOutputStream());
				
				System.out.println("Connection Received");
				
				//read json text from the sender
				String jsonText = inputStream.readUTF();
				
				System.out.println("Received input: " + jsonText);
				
				
				try {
					JSONObject object = (JSONObject) parser.parse(jsonText);
					
					//if a block was sent, determine validity and accept or reject
					if(object.get("type").equals("block")) {

						Block toAdd = new Block(object);
						
						System.out.println("Received block: "
							+ toAdd.getNum() + ":" + toAdd.getData());

						//add block to chain, send REJ if fail
						if (BlockchainManager.blockchain.addBlock(toAdd)) {
							
							System.out.println("Block accepted, sending ACC");
							outputStream.writeUTF("ACC");
						} else {
							
							System.out.println("Block rejected, sending REJ");
							outputStream.writeUTF("REJ");
						}

					//if chain exchange request was sent, instance with more valid chain sends their chain
					} else if (object.get("type").equals("chainexchange")) {
						long theirSize = (Long) object.get("size");
						long theirTime = (Long) object.get("time");
						
						System.out.println("Type: chainex, their length is "
							+ theirSize + ", their time is " + theirTime);
						
						//if most recent timestamp and size of chain are the same
						if (BlockchainManager.blockchain.size() == theirSize
						&& BlockchainManager.blockchain.getMostRecentTimestamp() == theirTime) {
							
							System.out.println("Size and time same as mine, sending agreement");
							
							//send agreement message
							outputStream.writeUTF("AGREE");

						} else {
							if(BlockchainManager.blockchain.size() > theirSize) {
								
								System.out.println("I'm more valid, sending chain");
								
								//I'm more valid, send chain
								outputStream.writeUTF("CHAIN");
								Sender.sendChain(inputStream, outputStream);

							} else if (BlockchainManager.blockchain.size() < theirSize) {
								
								System.out.println("They're more valid, requesting chain");
								
								//they're more valid, receive chain
								outputStream.writeUTF("REQ");
								receiveChain(inputStream, outputStream);
								
							} else if (BlockchainManager.blockchain.getMostRecentTimestamp() < theirTime) {
								
								System.out.println("I'm more valid, sending chain");
								
								//I'm more valid, send chain
								outputStream.writeUTF("CHAIN");
								Sender.sendChain(inputStream, outputStream);
			
							} else {
								
								System.out.println("They're more valid, requesting chain");
								
								//they're more valid, receive chain
								outputStream.writeUTF("REQ");
								receiveChain(inputStream, outputStream);
							}
						}
					}
					
				} catch (ParseException parex) {
					parex.printStackTrace();
				}
				

				outputStream.flush();
				
				inputStream.close();
				outputStream.close();
				
				receiveSocket.close();
				
			} catch (IOException ioex) {
				ioex.printStackTrace();
			}
		}
	}
}