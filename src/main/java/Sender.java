import java.io.*;
import java.net.*;

import org.json.simple.JSONObject;
//import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


public class Sender {
	
	public static void sendChain(DataInputStream inputStream, DataOutputStream outputStream)
	throws IOException {

		int i = 0;
		String response = "";
		
		System.out.println("Sending chain...");

		//send blocks until done or rejected
		while (i < BlockchainManager.blockchain.size() && !response.equals("REJ")) {
			//send block
			outputStream.writeUTF(
				BlockchainManager.blockchain.getBlock(i).toJsonString()
			);
			
			System.out.println("   Sent block " + i + ":" + BlockchainManager.blockchain.getBlock(i).getData());
			
			//wait for ACC or REJ for each block
			response = inputStream.readUTF();
			
			System.out.println("   Received back: " + response);
			
			i++;
		}
		
		//if end of chain reached without rejection, send "done" message
		if (i == BlockchainManager.blockchain.size()) {
			outputStream.writeUTF("DONE");
		}
		
		System.out.println("Send Complete");
	}
	
	public static void sendChainExchange(InetAddress recipientAddress, int port) {
		
		try {
			
			System.out.println("Creating request object...");
		
			//create json object with chain request, size of my chain, and timestamp
			JSONObject object = new JSONObject();
			object.put("type", "chainexchange");
			object.put("size", BlockchainManager.blockchain.size());
			object.put("time", BlockchainManager.blockchain.getMostRecentTimestamp());
			String chainEx = object.toString();
			
			System.out.println("Connecting to recipient...");
			
			//connect to recipient
			Socket sendSocket = new Socket(recipientAddress, port);
			
			System.out.println("Connected!");
			
			DataInputStream inputStream = new DataInputStream(sendSocket.getInputStream());
			DataOutputStream outputStream = new DataOutputStream(sendSocket.getOutputStream());
			
			System.out.println("Sending exchange request object...");
			
			//send request for chain to recipient
			outputStream.writeUTF(chainEx);
			
			System.out.println("Sent, Awaiting Response...");
			
			//wait for response
			String response = inputStream.readUTF();
			
			System.out.println("Received response: " + response);
			
			if (response.equals("AGREE")) {
				//do nothing?
			} else if (response.equals("REQ")) {
				sendChain(inputStream, outputStream);
			} else if (response.equals("CHAIN")) {
				Receiver.receiveChain(inputStream, outputStream);
			}
			

			outputStream.flush();
			
			inputStream.close();
			outputStream.close();
			
			sendSocket.close();
			
		} catch (IOException ioex) {
			ioex.printStackTrace();
		}
	}
}