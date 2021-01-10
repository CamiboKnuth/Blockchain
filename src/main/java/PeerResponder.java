import java.net.*;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class PeerResponder extends Thread {
	
	private MulticastSocket udpSocket;
	
	//multicast address
	private InetAddress groupAddress;

	//port to which udpSocket is bound
	private int bindPort;
	
	//identifies state of thread
	private volatile int responderFlag;
	
	private final int DEFAULT_FLAG = 0;
	private final int CLOSE_FLAG = 1;
	
	public PeerResponder(MulticastSocket udpSocket, InetAddress groupAddress) {
		
		this.udpSocket = udpSocket;
		this.groupAddress = groupAddress;

		//set thread to default state
		responderFlag = DEFAULT_FLAG;
	}
	
	public void obtainPeerList() {
		JSONParser parser = new JSONParser();

		String request = "PEER_REQ";

		//first packet goes to port determined by (CHAIN_BASE_PORT + UDP_OFFSET)
		DatagramPacket peerPacket =
			new DatagramPacket(	request.getBytes(),
								request.getBytes().length,
								groupAddress,
								BlockchainManager.CHAIN_BASE_PORT
									+ BlockchainManager.UDP_OFFSET);
								
		//send peer request packet to multicast address on range of udp ports
		for(int i = 0; i < BlockchainManager.UDP_OFFSET; i++) {
			udpSocket.send(peerPacket);
			peerPacket.setPort(peerPacket.getPort() + 1);
		}
		
		boolean timeoutFlag = false;
		
		//until timeout, add all responders to peer list
		while(!timeoutFlag) {
			try {
				byte[] buffer = new byte[128];
				DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
				
				//wait for peer response packet
				udpSocket.receive(receivePacket);		
				
				JSONObject object =
					(JSONObject) parser.parse(
					new String(receivePacket.getData(),"UTF-8").trim());
				
				//get host and port from response packet
				String host = object.get("ip");
				int port = object.get("port");
				
				//if host is not empty, add to peer list
				if(!host.equals("")) {
					BlockchainManager.peerList.add(new InetSocketAddress(host, port));
				}
			} catch (SocketTimeoutException stex) {
				timeoutFlag = true;
			}
		}
	}
	
	public void run() {
		
		while (responderFlag != CLOSE_FLAG) 
			try {
				byte[] buffer = new byte[128];
				DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
				
				//wait for peer request packet
				udpSocket.receive(receivePacket);
				
				//if packet is a peer request, respond with host and port of this instance
				if((new String(receivePacket.getData(),"UTF-8")).trim().equals("PEER_REQ")) {
					
					//add new peer to peer list
					BlockchainManager.peerList.add(
						new InetSocketAddress(receivePacket.getAddress(),
						receivePacket.getPort() - BlockchainManager.UDP_OFFSET));
					
					//obtain local address
					String address = "";
					try {
						address = Inet4Address.getLocalHost().getHostAddress();
					} catch (UnknownHostException uhex) {
						uhex.printStackTrace();
					}
				
					//add address and port to json string
					JSONObject object = new JSONObject();
					object.put("ip", address);
					object.put("port", BlockchainManager.bindPort);
				
					byte[] toSend = object.toString().getBytes();
					
					DatagramPacket peerPacket =
						new DatagramPacket(	toSend,
											toSend.length,
											receivePacket.getAddress(),
											receivePacket.getPort());	
							
					//send response back to sender
					udpSocket.send(peerPacket);
				}
			} catch (SocketTimeoutException stex) {
				System.out.println("NOTE: ROUTINE UDP TIMEOUT");
			}
		}
	}
}