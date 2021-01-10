import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Block {
	//identifying number of block
	private long blockNum;
	
	//time when block was created
	private long timestamp;
	
	//hash of previous block
	private String prevHash;
	
	//data contained in block
	private String data;
	
	//unique value to create block hash
	private long nonce;
		

	//construct block with id number, hash of previous block, data to contain
	public Block(long blockNum, String prevHash, String data) {
		this.blockNum = blockNum;
		this.timestamp = System.currentTimeMillis();
		this.prevHash = prevHash;
		this.data = data;
		
		//temporarily set nonce to zero
		this.nonce = 0;
	}
	
	//turn block into a valid block
	public void mineBlock() throws NoSuchAlgorithmException {
		//create digest to create SHA-256 hashes
		MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
		
		String hexHash = "";
		
		//loop until hexHash starts with a certain number of zeros
		do {
			//augment and check next nonce
			this.nonce ++;
			
			//hash blocknum, timestamp, prevhash, data, and nonce together
			String toHash = Integer.toString(this.blockNum) + Long.toString(this.timestamp)
				+ prevHash + data + Integer.toString(nonce);
			messageDigest.update(toHash.getBytes());
			
			//get hexadecimal string version of hash
			hexHash = bytesToHex(messageDigest.digest());	
		
		} while(!hexHash.substring(0,Blockchain.proofOfWorkString.length()).equals(
			Blockchain.proofOfWorkString));
		
		System.out.println("Block Mined Successfully:");
		System.out.println("   block number    : " + this.blockNum);
		System.out.println("   timestamp is    : " + this.timestamp);
		System.out.println("   previous hash is: " + this.prevHash);
		System.out.println("   contains data   : " + this.data);
		System.out.println("   nonce value is  : " + this.nonce);
		System.out.println("   block hash is   : " + this.calculateHash());
	}
	
	public long getNum() {
		return this.blockNum;
	}
	
	public long getTimestamp() {
		return this.timestamp;
	}
	
	public String getPrevHash() {
		return this.prevHash;
	}
	
	public String getData() {
		return this.data;
	}

	public long getNonce() {
		return this.nonce;
	}
	
	//check block validity
	public boolean isValid() throws NoSuchAlgorithmException {		
		//get hexadecimal string version of hash
		String hexHash = this.calculateHash();

		//return true if hash string has appropriate number of zeros
		return (hexHash.substring(0,Blockchain.proofOfWorkString.length()).equals(
			Blockchain.proofOfWorkString));
	}
	
	public String calculateHash() throws NoSuchAlgorithmException {
		//create digest to create SHA-256 hashes
		MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
		
		//hash blocknum, timestamp, prevhash, data, and nonce together
		String toHash = Integer.toString(this.blockNum) + Long.toString(this.timestamp)
			+ prevHash + data + Integer.toString(nonce);
		messageDigest.update(toHash.getBytes());
		
		//get hexadecimal string version of hash
		return bytesToHex(messageDigest.digest());
	}
	
	//convert array of bytes to a hexadecimal string
	private static String bytesToHex(byte[] byteArray) {
		StringBuilder builder = new StringBuilder();
		
		for (int i = 0; i < byteArray.length; i++) {
			builder.append(String.format("%02x", byteArray[i]));
		}
		
		return builder.toString();
	}
}