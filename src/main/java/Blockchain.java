import java.util.ArrayList;
import java.security.NoSuchAlgorithmException;

public class Blockchain {
	
	public static final String proofOfWorkString = "000";
	
	private ArrayList<Block> blockList;
	private long size;
	
	public Blockchain() {
		this.blockList = new ArrayList<Block>();
		this.size = 0;
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
	
	public Block getBlock(long index) {
		return blockList.get(index);
	}
}