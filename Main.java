import java.security.NoSuchAlgorithmException;

public class Main {
	public static void main(String[] args) throws NoSuchAlgorithmException {
		
		//create previous hash for first block
		String firstPrevHash = "00000000000000000000000000000000" 
			+ "00000000000000000000000000000000";
		
		//create first block
		Block block = new Block(0, firstPrevHash, "block data test");
		
		//check block validity before mining
		System.out.println("before mining, is block valid? " + block.isValid());
		System.out.println("begin mining block...");
		
		//mine block
		block.mineBlock();
		
		//check block validity after mining
		System.out.println("after mining, is block valid? " + block.isValid());
	}
}