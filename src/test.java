import java.util.ArrayList;

public class test
{
	public static void main(String[] args)
	{
		HuffmanTree ht = new HuffmanTree("testin.txt","testout.huf",true);
		HuffmanTree htback = new HuffmanTree("testout.huf","testback.txt");
//		HuffmanTree ht = new HuffmanTree("unbreakable-napped.txt","unbreakable-napped.huf",true);
//		HuffmanTree htback = new HuffmanTree("unbreakable-napped.huf","unbreakable-back.txt");
	}
}
