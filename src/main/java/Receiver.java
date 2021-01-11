import java.net.*;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class Receiver extends Thread {
	
	private ServerSocket serverSocket;
	private JSONParser parser;
	
	
	private volatile int receiverFlag;
	
	private final int DEFAULT_FLAG = 0;
	private final int CLOSE_FLAG = 1;
	
	public Receiver(ServerSocket serverSocket) {
		this.serverSocket = serverSocket;
		this.parser = new JSONParser();
	}
	
	public static void receiveChain(DataInputStream inputStream, DataOutputStream outputStream) {
		
		//create new chain to compare to current chain
		Blockchain nextChain = new Blockchain();
		
		boolean done = false;
		
		//accept first input from other user
		String response = inputStream.readUTF();
		
		while (!done) {
			if (!response.equals("DONE")) {

				//parse block string into json object
				JSONObject object = (JSONObject) parser.parse(response);
				
				//handle if block, send REJ if not
				if(object.get("type").equals("block")) {
					
					//create block from sent json object
					Block nextBlock = new Block(object);
					
					//add block to chain, send REJ if fail
					if (nextChain.addBlock(nextBlock)) {
						outputStream.writeUTF("ACC");
					} else {
						outputStream.writeUTF("REJ");
						done = true;
					}
					
				} else {
					outputStream.writeUTF("REJ");
					done = true;
				}
				
				response = inputStream.readUTF();

			} else {
				done = true;
			}
		}
		
		//Compare chains and keep more valid chain
		
		if (BlockchainManager.blockchain.size() < nextChain.size()) {
			//they're more valid, replace chain
			BlockchainManager.blockchain = nextChain;
			
		} else if (BlockchainManager.blockchain.size() == nextChain.size()) {

			if (BlockchainManager.blockchain.getMostRecentTimestamp()
			> nextChain.getMostRecentTimestamp()) {

				//they're more valid, replace chain
				BlockchainManager.blockchain = nextChain;
			}	
		} 		
	}
	
	public void run() {
		
		//TODO: handle multiple connections?
		
		
		while(receiverFlag != CLOSE_FLAG) {
			//wait to receive connection
			Socket receiveSocket = serverSocket.accept();
			DataInputStream inputStream = new DataInputStream(receiveSocket.getInputStream());
			DataOutputStream outputStream = new DataOutputStream(receiveSocket.getOutputStream());
			
			//read json text from the sender
			String jsonText = inputStream.readUTF();
			JSONObject object = (JSONObject) parser.parse(jsonText);
			
			//if a block was sent, determine validity and accept or reject
			if(object.get("type").equals("block")) {
				
				//TODO: validate block
			
			//if chain exchange request was sent, instance with more valid chain sends their chain
			} else if (object.get("type").equals("chainexchange")) {
				long theirSize = (Long) object.get("size");
				long theirTime = (Long) object.get("time");
				
				//if most recent timestamp and size of chain are the same
				if (blockchain.size() == theirSize
				&& blockchain.getMostRecentTimestamp() == theirTime) {
					
					//send agreement message
					outputStream.writeUTF("AGREE");

				} else {
					if(BlockchainManager.blockchain.size() > theirSize) {
						//I'm more valid, send chain
						outputStream.writeUTF("CHAIN");
						Sender.sendChain(inputStream, outputStream);

					} else if (BlockchainManager.blockchain.size() < theirSize) {
						//they're more valid, receive chain
						outputStream.writeUTF("REQ");
						receiveChain(inputStream, outputStream);
						
					} else if (BlockchainManager.blockchain.getMostRecentTimestamp() < theirTime) {
						//I'm more valid, send chain
						outputStream.writeUTF("CHAIN");
						Sender.sendChain(inputStream, outputStream);
	
					} else {
						//they're more valid, receive chain
						outputStream.writeUTF("REQ");
						receiveChain(inputStream, outputStream);
					}
				}
			} else {
				//do nothing?
			}
			
			inputStream.flush();
			outputStream.flush();
			
			inputStream.close();
			outputStream.close();
			
			receiveSocket.close();
		}
	}
}