import java.net.*;

import org.json.simple.JSONObject;
//import org.json.simple.JSONValue;

public class Sender {
	
	//private Socket sendSocket;
	
	public Sender() {
		//sendSocket = new Socket();
	}
	
	public void chainExchange(InetAddress recipientAddress, port) {
		
		//create json object with chain request, size of my chain, and timestamp
		JSONObject object = new JSONObject();
		object.put("type", "chainexchange");
		object.put("size", BlockchainManager.blockchain.size());
		object.put("time", BlockchainManager.blockchain.getMostRecentTimestamp())
		String chainReq = object.toString();
		
		//connect to recipient
		Socket sendSocket = new Socket(recipientAddress, port);
		
		DataInputStream inputStream = new DataInputStream(sendSocket.getInputStream());
		DataOutputStream outputStream = new DataOutputStream(sendSocket.getOutputStream());
		
		//send request for chain to recipient
		outputStream.writeUTF(chainReq);
		
		//wait for response
		String response = inputStream.readUTF();
		
		if (response.equals("AGREE")) {
			
		} else if () {
			
		} else {
			
		}
		
		inputStream.flush();
		outputStream.flush();
		
		inputStream.close();
		outputStream.close();
		
		sendSocket.close();
	}

	
	public void sendAccept(Block block) {
		
		JSONObject object = new JSONObject();
		object.put("type", "acc");
		object.put("num", block.getNum());
		object.put("time", block.getTimestamp());
		object.put("hash", block.calculateHash());
		
		sendBuffer(object.toString().getBytes());	
	}
	
	public void sendBlock(Block block) {
		JSONObject object = new JSONObject();
		object.put("type", "block");
		object.put("num", block.getNum());
		object.put("time", block.getTimestamp());
		object.put("prevhash", block.getPrevHash());
		object.put("data", block.getData());
		object.put("nonce", block.getNonce());
		
		sendBuffer(object.toString().getBytes());		
	}
	
}