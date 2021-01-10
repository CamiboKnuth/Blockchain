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
			
			
			//if chain exchange request was sent, instance with more valid chain sends their chain
			} else if (object.get("type").equals("chainexchange")) {
				long theirSize = (Long) object.get("size");
				long theirTime = (Long) object.get("time");
				
				if (blockchain.size() == theirSize
				&& blockchain.getMostRecentTimestamp() == theirTime) {
					
					//send agreement message
					outputStream.writeUTF("AGREE");

				} else {
					if(blockchain.size() > theirSize) {
						//im more valid
						//send chain
					} else if (blockchain.size() < theirSize) {
						//they're more valid
						//request chain
					} else if (blockchain.getMostRecentTimestamp() < theirTime) {
						//im more valid
						//send chain
					} else {
						//they're more valid
						//request chain
					}
				}
			} else {
				//Do Nothing?
			}
			
			inputStream.flush();
			outputStream.flush();
			
			inputStream.close();
			outputStream.close();
			
			receiveSocket.close();
		}
	}
}