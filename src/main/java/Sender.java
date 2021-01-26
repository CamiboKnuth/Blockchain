import java.io.*;
import java.net.*;
import java.util.*;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


public class Sender {
	
	public static void sendChain(DataInputStream inputStream, DataOutputStream outputStream)
	throws IOException {

		int i = 0;
		String response = "";
		
		System.out.println("Sending chain...");

		//send blocks in chain to other user until done or rejected
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

	public static boolean sendChainExchange(InetSocketAddress recipientAddress) {
		
		boolean swapped = false;
		
		try {
			System.out.println("Creating exchange request object...");
		
			//create json object with chain request, size of my chain, and timestamp
			JSONObject object = new JSONObject();
			object.put("type", "chainexchange");
			object.put("size", BlockchainManager.blockchain.size());
			object.put("time", BlockchainManager.blockchain.getMostRecentTimestamp());
			String chainEx = object.toString();
			
			System.out.println("Connecting to recipient...");
			
			//connect to recipient
			Socket sendSocket = new Socket();
			sendSocket.setSoTimeout(500);
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
			
			//if they agree with local chain, do nothing
			if (response.equals("AGREE")) {
				//do nothing
				
			//if they request local chain, send local chain to them
			} else if (response.equals("REQ")) {
				sendChain(inputStream, outputStream);
				
			//if they request to send chain, prepare to receive chain
			} else if (response.equals("CHAIN")) {
				swapped = Receiver.receiveChain(inputStream, outputStream);
			}
			
			//close connection with other user
			outputStream.flush();
			
			inputStream.close();
			outputStream.close();
			
			sendSocket.close();
			
		} catch (SocketTimeoutException stex) {
			System.out.println("TIMEOUT");
		} catch (ConnectException conex) {
			System.out.println("Connection Failed.");
		} catch (IOException ioex) {
			ioex.printStackTrace();
		}
		
		return swapped;
	}
	
	//offer to exchange change with each peer
	public static void findBestChainFromPeers(ArrayList<InetSocketAddress> peerList) {
		for(int i = 0; i < peerList.size(); i++) {
			sendChainExchange(peerList.get(i));	
		}
	}
	
	public static boolean broadcastBlock(Block block, ArrayList<InetSocketAddress> peerList) {
		boolean success = true;
		
		try {
			System.out.println("Creating block object to broadcast...");
			
			//convert block to be broadcast to json string
			String blockString = block.toJsonString();
			
			System.out.println("Broadcasting block...");
			
			//create list with same data as peer list
			ArrayList<InetSocketAddress> newPeerList = new ArrayList<InetSocketAddress>(peerList);
			
			//loop over list, sending block to each, removing peers from
			//original list if connection refused
			int i = 0;
			while (i < newPeerList.size() && success) {

				try {
					//connect to recipient
					Socket sendSocket = new Socket();
					sendSocket.setSoTimeout(500);
					sendSocket.connect(newPeerList.get(i));
					
					DataInputStream inputStream = new DataInputStream(sendSocket.getInputStream());
					DataOutputStream outputStream = new DataOutputStream(sendSocket.getOutputStream());
					
					System.out.println("Sending block to recipient " + i + "...");
					
					//send request for chain to recipient
					outputStream.writeUTF(blockString);
					
					System.out.println("Sent, Awaiting Response...");
					
					//wait for response
					String response = inputStream.readUTF();
					
					System.out.println("Received response: " + response);
					
					
					//close connection with other user
					outputStream.flush();
					
					inputStream.close();
					outputStream.close();
					
					sendSocket.close();
				
					//if other user rejected block, request their chain.
					//retain success if local chain is not replaced
					if (response.equals("REJ")) {
						System.out.println("Rejected, exchanging chain...");
						
						success = !sendChainExchange(newPeerList.get(i));
						
						System.out.println("Was chain swapped?: " + !success);
					}

				} catch (SocketTimeoutException stex) {
					System.out.println("TIMEOUT");
				} catch (ConnectException conex) {
					System.out.println("Connection Failed. Removing Peer.");
					peerList.remove(newPeerList.get(i));
				}
				
				i++;
			}
			
			//if chain broadcast failed, find best chain from peers
			if (!success) {
				System.out.println("Broadcast failed, finding best chain from peers...");
				findBestChainFromPeers(peerList);
			}
			
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return success;	
	}
}