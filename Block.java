import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Block {
	//identifying number of block
	private int blockNum;
	
	//data contained in block
	private String data;
	
	//hash of previous block
	private String prevHash;
	
	//unique value to create block hash
	private int nonce;
	
	
	//hash of this block
	private String blockHash;
	

	//construct block with id number, hash of previous block, data to contain
	public Block(int blockNum, String prevHash, String data) {
		this.blockNum = blockNum;
		this.prevHash = prevHash;
		this.data = data;
		this.nonce = 0;
		//start hash with all zeros before mining
		this.blockHash = "00000000000000000000000000000000" 
			+ "00000000000000000000000000000000";
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
			
			//hash blocknum, data, prevhash, and nonce together
			String toHash = Integer.toString(this.blockNum) + data + prevHash
				+ Integer.toString(nonce);
			messageDigest.update(toHash.getBytes());
			
			//get hexadecimal string version of hash
			hexHash = bytesToHex(messageDigest.digest());	
		
		} while(!hexHash.substring(0,3).equals("000"));
		
		//set this blocks hash to the determined hash with the zeros
		this.blockHash = hexHash;
		
		System.out.println("Block Mined Successfully:");
		System.out.println("   block number    : " + this.blockNum);
		System.out.println("   contains data   : " + this.data);
		System.out.println("   nonce value is  : " + Integer.toString(this.nonce));
		System.out.println("   previous hash is: " + this.prevHash);
		System.out.println("   block hash is   : " + this.blockHash);
	}
	
	//check block validity
	public boolean isValid() throws NoSuchAlgorithmException {
		//create digest to create SHA-256 hashes
		MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
		
		//hash blocknum, data, prevhash, and nonce together
		String toHash = Integer.toString(this.blockNum) + data + prevHash
			+ Integer.toString(nonce);
		messageDigest.update(toHash.getBytes());
		
		//get hexadecimal string version of hash
		String hexHash = bytesToHex(messageDigest.digest());

		//return true if hash string has appropriate number of zeros
		return (hexHash.substring(0,3).equals("000"));
	}
	
	public int getNum() {
		return this.blockNum;
	}
	
	public String getData() {
		return this.data;
	}
	
	public String getPreviousHash() {
		return this.prevHash;
	}
	
	public int getNonce() {
		return this.nonce;
	}
	
	public String getHash() {
		return this.blockHash;
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