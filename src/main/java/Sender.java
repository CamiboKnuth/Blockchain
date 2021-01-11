import java.net.*;

import org.json.simple.JSONObject;
//import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


public class Sender {
	
	public static void sendChain(DataInputStream inputStream, DataOutputStream outputStream) {

		long i = 0;
		String response = "";

		//send blocks until done or rejected
		while (i < BlockchainManager.blockchain.size() && !response.equals("REJ")) {
			//send block
			outputStream.writeUTF(
				BlockchainManager.blockchain.getBlock(i).toJsonString()
			);
			
			//wait for ACC or REJ for each block
			response = inputStream.readUTF();
			
			i++;
		}
		
		//if end of chain reached without rejection, send "done" message
		if (i == BlockchainManager.blockchain.size()) {
			outputStream.writeUTF("DONE");
		}
	}
	
	public static void sendChainExchange(InetAddress recipientAddress, port) {
		
		//create json object with chain request, size of my chain, and timestamp
		JSONObject object = new JSONObject();
		object.put("type", "chainexchange");
		object.put("size", BlockchainManager.blockchain.size());
		object.put("time", BlockchainManager.blockchain.getMostRecentTimestamp());
		String chainEx = object.toString();
		
		//connect to recipient
		Socket sendSocket = new Socket(recipientAddress, port);
		
		DataInputStream inputStream = new DataInputStream(sendSocket.getInputStream());
		DataOutputStream outputStream = new DataOutputStream(sendSocket.getOutputStream());
		
		//send request for chain to recipient
		outputStream.writeUTF(chainEx);
		
		//wait for response
		String response = inputStream.readUTF();
		
		if (response.equals("AGREE")) {
			//do nothing?
		} else if (response.equals("REQ")) {
			sendChain(inputStream, outputStream);
		} else if (response.equals("CHAIN")) {
			Receiver.receiveChain(inputStream, outputStream);
		}
		
		inputStream.flush();
		outputStream.flush();
		
		inputStream.close();
		outputStream.close();
		
		sendSocket.close();
	}
}