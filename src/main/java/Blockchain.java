import java.util.ArrayList;
import java.security.NoSuchAlgorithmException;

public class Blockchain {
	
	public static final String proofOfWorkString = "0000";
	
	private ArrayList<Block> blockList;
	private int size;
	
	public Blockchain() {
		this.blockList = new ArrayList<Block>();
		this.size = 0;
	}
	
	public ArrayList<Block> getBlockList() {
		return blockList;
	}
	
	public Block getBlockFromData(String data) {
		
		Block newBlock = null;
		
		if(size == 0) {
			String firstPrevHash = "00000000000000000000000000000000" 
				+ "00000000000000000000000000000000";

			newBlock = new Block(size, firstPrevHash, data);
		} else {
			newBlock = new Block(size, blockList.get(size - 1).calculateHash(), data);
		}
		
		return newBlock;
	}
	
	
	//for test purposes
	public void addInvalidBlock(Block toAdd) {
		blockList.add(toAdd);
		this.size ++;		
	}
	
	public void removeLastBlock() {
		blockList.remove(size - 1);
		this.size --;
	}
	
	public boolean addBlock(Block toAdd) {
		
		boolean success = false;
		
		//check if block hash has appropriate number of 0s at beginning
		if(toAdd.isValid()) {

			//if no blocks, first block is genesis block with all 0s for prev hash
			if(this.size == 0) {
				String firstPrevHash = "00000000000000000000000000000000" 
					+ "00000000000000000000000000000000";
					
				//first block must have all 0s as prev hash to be added
				if(toAdd.getPrevHash().equals(firstPrevHash)) {
					blockList.add(toAdd);
					this.size ++;
					success = true;
				}
				
			//if there are some blocks already, prev hash of new block must be hash
			//of previous block
			} else {
				Block lastBlock = blockList.get(size - 1);
				
				if(toAdd.getPrevHash().equals(lastBlock.calculateHash())) {
					blockList.add(toAdd);
					this.size ++;
					success = true;
				}
			}
		}
		
		return success;
	}
	
	public void toLongestValidChain() {
		Blockchain newChain = new Blockchain();
		
		int i = 0;
		
		while(i < this.size && newChain.addBlock(this.getBlock(i))) {
			i++;
		}
		
		this.blockList = newChain.getBlockList();
		this.size = newChain.size();
	}
	
	public Block getBlock(int index) {
		return blockList.get(index);
	}
	
	public long getMostRecentTimestamp() {
		long result = 0;
		
		if (size > 0) {
			result = blockList.get(size - 1).getTimestamp();
		}
		return result;
	}
	
	public int size() {
		return this.size;
	}
	
	public String toHtmlString() {
		StringBuilder toReturn = new StringBuilder("******************************<br>");
		
		for (int i = size - 1; i >= 0; i--) {
			toReturn.append("Block #" + blockList.get(i).getNum() + "<br>"); 
			toReturn.append("   Previous Block Hash: " + blockList.get(i).getPrevHash() + "<br>");
			toReturn.append("   Block Hash:          " + blockList.get(i).calculateHash() + "<br>");
			toReturn.append("   Timestamp:           " + blockList.get(i).getTimestamp() + "<br>");
			toReturn.append("   Nonce:               " + blockList.get(i).getNonce() + "<br>");
			toReturn.append("   Data:                " + blockList.get(i).getData() + "<br>");
			toReturn.append("******************************<br>");
		}
		
		return toReturn.toString();
	}
	
	public void printChain() {
		for (int i = 0; i < size; i++) {
			System.out.println("   " + blockList.get(i).getNum() + ":"
				+ blockList.get(i).getData());
		}
	}
}