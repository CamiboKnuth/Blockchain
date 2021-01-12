import java.util.ArrayList;
import java.security.NoSuchAlgorithmException;

public class Blockchain {
	
	public static final String proofOfWorkString = "000";
	
	private ArrayList<Block> blockList;
	private int size;
	
	public Blockchain() {
		this.blockList = new ArrayList<Block>();
		this.size = 0;
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
	
	public long size() {
		return this.size;
	}
	
	public void printChain() {
		for (int i = 0; i < size; i++) {
			System.out.println("   " + blockList.get(i).getNum() + ":" + blockList.get(i).getData());
		}
	}
}