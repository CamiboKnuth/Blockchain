import java.io.*;
import java.net.*;
import java.util.*;

import org.json.simple.JSONObject;
//import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


public class Sender {
	
	
	//TODO: handle timeout
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
		
		outputStream.writeUTF("DONE");
		
		//if end of chain reached with rejection, validate my chain
		if (response.equals("REJ")) {
			BlockchainManager.blockchain.toLongestValidChain();
			System.out.println("Removed invalid blocks from my chain.");
			
		}
		
		System.out.println("Send Complete");
	}
	
	
	//TODO: handle case where peer no longer online
	//TODO: handle timeout
	public static boolean sendChainExchange(InetSocketAddress recipientAddress) {
		
		boolean swapped = false;
		
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
			Socket sendSocket = new Socket();
			sendSocket.connect(recipientAddress);
			
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
				swapped = Receiver.receiveChain(inputStream, outputStream);
			}
			
			outputStream.flush();
			
			inputStream.close();
			outputStream.close();
			
			sendSocket.close();
			
		} catch (IOException ioex) {
			ioex.printStackTrace();
		}
		
		return swapped;
	}
	
	public static void findBestChainFromPeers(ArrayList<InetSocketAddress> peerList) {
		for(int i = 0; i < peerList.size(); i++) {
			sendChainExchange(peerList.get(i));	
		}
	}
	
	//TODO: handle case where peer no longer online
	public static boolean broadcastBlock(Block block, ArrayList<InetSocketAddress> peerList) {
		
		boolean success = true;
		
		try {
			System.out.println("Creating block object to broadcast...");
		
			String blockString = block.toJsonString();
			
			System.out.println("Broadcasting block...");
			
			int i = 0;
			
			while (i < peerList.size() && success) {

				//connect to recipient
				Socket sendSocket = new Socket();
				sendSocket.connect(peerList.get(i));
				
				DataInputStream inputStream = new DataInputStream(sendSocket.getInputStream());
				DataOutputStream outputStream = new DataOutputStream(sendSocket.getOutputStream());
				
				System.out.println("Sending block to recipient " + i + "...");
				
				//send request for chain to recipient
				outputStream.writeUTF(blockString);
				
				System.out.println("Sent, Awaiting Response...");
				
				//wait for response
				String response = inputStream.readUTF();
				
				System.out.println("Received response: " + response);
				
				outputStream.flush();
				
				inputStream.close();
				outputStream.close();
				
				sendSocket.close();
			
				if (response.equals("REJ")) {
					success = !sendChainExchange(peerList.get(i));
				}
				
				i++;
			}
			
			//may not be necessary?
			if (!success) {
				findBestChainFromPeers(peerList);
			}
			
		} catch (Exception ex) {
			success = false;
			ex.printStackTrace();
		}

		return success;
		
	}
}