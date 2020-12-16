import java.net.*;

public class Receiver extends Thread {
	
	private ServerSocket serverSocket;
	
	
	private volatile int receiverFlag;
	
	private final int DEFAULT_FLAG = 0;
	private final int CLOSE_FLAG = 1;
	
	public Receiver(ServerSocket serverSocket) {
		this.serverSocket = serverSocket;
	}
	
	public void run() {
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
				
			} else {
				
			}
		}
	}
}