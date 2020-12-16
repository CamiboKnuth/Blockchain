import java.net.*;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;


public class Sender {
	
	private Socket sendSocket;
	
	public Sender() {
		sendSocket = socket;
	}
	
	public void sendChainRequest(InetAddress recipientAddress, port) {
		JSONObject object = new JSONObject();
		object.put("type", "chain");
		
		byte[] toSend = object.toString().getBytes();
		
		DatagramPacket chainReqPacket =
			new DatagramPacket(	toSend,
								toSend.length,
								recipientAddress,
								port);	
								
		udpSendSocket.send(chainReqPacket);	
	}

	
	public void sendAccept(Block block) {
		
		JSONObject object = new JSONObject();
		object.put("type", "acc");
		object.put("num", block.getNum());
		object.put("hash", block.calculateHash());
		
		sendBuffer(object.toString().getBytes());	
	}
	
	public void sendBlock(Block block) {
		JSONObject object = new JSONObject();
		object.put("type", "block");
		object.put("num", block.getNum());
		object.put("prevhash", block.getPrevHash());
		object.put("data", block.getData());
		object.put("nonce", block.getNonce());
		
		sendBuffer(object.toString().getBytes());		
	}
	
}