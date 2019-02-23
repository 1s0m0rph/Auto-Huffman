//import java.io.FileNotFoundException;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Arrays;
import java.util.Stack;

//TODO: switch over from using 8 bit ascii chars in .huf files to 16 bit unicode

public class HuffmanTree
{
	class AmbiguousSymbolException extends IllegalArgumentException
	{
		public AmbiguousSymbolException(String msg){super(msg);}
	}
	
	static final char META_OPEN_RENDER = '|';//for rendering purposes
	static final char META_OPEN = 0;
//	static final char TREE_POP = ']';
	Symbol[] alphabet;//want to keep track of this for quick access; this should be sorted by sym and ONLY contain regular symbols
	Symbol root;
	
	public HuffmanTree(){}
	
	/*
	Read the tree/message from this file. output the de/encoded message to outfile
	 */
	public HuffmanTree(String filename, String outfile,boolean verbose)
	{
		if(filename.substring(filename.length()-4).equals(".huf"))//file is encoded
			readEncodedFile(filename,outfile);
		else
			encodeFileAndOutputToFile(filename,outfile);
		if(verbose)
			show();
	}
	
	/*
	Read the tree/message from this file. output the de/encoded message to outfile
	 */
	public HuffmanTree(String filename, String outfile)
	{
		if(filename.substring(filename.length()-4).equals(".huf"))//file is encoded
			readEncodedFile(filename,outfile);
		else
			encodeFileAndOutputToFile(filename,outfile);
	}
	
	public HuffmanTree(ArrayList<Symbol> alpha)
	{
		runHF(alpha);
	}
	
	void initAlphabet(ArrayList<Symbol> alpha, int size)
	{
		this.alphabet = new Symbol[size];
		int i = 0;
		for(Symbol s : alpha)
		{
//			if(s.sym == META_OPEN)
//				throw new AmbiguousSymbolException("The tree metasymbol-open symbol occurred somewhere in the text.");
			if(s.sym != 0)
				this.alphabet[i++] = s;
		}
		Arrays.sort(this.alphabet, new Symbol());
	}
	
	void initAlphabet(ArrayList<Symbol> alpha)
	{
		initAlphabet(alpha,alpha.size());
	}
	
	void runHF(ArrayList<Symbol> alpha)
	{
		initAlphabet(alpha);
		int numNodes = alpha.size();
		while(numNodes > 1)
		{
			//run huffman iteratively
			int minidx1 = 0;
			int minidx2 = 1;
			//^^ 2 smallest frequency symbols
			for(int i = 1; i < alpha.size(); i++)
			{
				if(alpha.get(i).frequency < alpha.get(minidx1).frequency)
				{
					minidx2 = minidx1;
					minidx1 = i;
				}
				else if(alpha.get(i).frequency < alpha.get(minidx2).frequency)
					minidx2 = i;
			}
			
			Symbol meta = new Symbol(alpha.get(minidx1),alpha.get(minidx2),alpha.get(minidx1).frequency + alpha.get(minidx2).frequency);
			alpha.get(minidx1).parent = meta;
			alpha.get(minidx2).parent = meta;
			alpha.add(meta);
			alpha.get(minidx1).frequency = Integer.MAX_VALUE;//surrogate for deletion for the sake of speed
			alpha.get(minidx2).frequency = Integer.MAX_VALUE;//surrogate for deletion for the sake of speed
			numNodes--;
			if(numNodes == 1)
			{
				//meta is the root
				this.root = meta;
			}
		}
	}
	
	private Symbol getSymbolForChar(char c, int low, int high)
	{
		//bin search on alphabet for the correct Symbol
		int med = ((high + low) >> 1);
		if(alphabet[med].sym == c)
			return alphabet[med];
		if(high <= low)
			throw new IllegalArgumentException("Character " + c + " not in alphabet.");
		
		if((int)c < (int)alphabet[med].sym)
			return getSymbolForChar(c,low,med-1);
		return getSymbolForChar(c,med+1,high);
	}
	
	String decode(String s)
	{
		//continuous traversal of the huffman tree
		Symbol current = root;
		StringBuilder ret = new StringBuilder();
		for(int i = 0; i < s.length(); i++)
		{
			char c = s.charAt(i);
			if(current == null)
			{
				return ret.toString();
				//this can happen when we reach the end of a message that was encoded bytewise since they have to pad with zeroes at the end
			}
			if(current.sym != 0)
			{
				//we've found the node and can add the correct letter
				ret.append(current.sym);
				current = c == '0' ? root.left : root.right;
			}
			else
			{
				//update current
				current = c == '0' ? current.left : current.right;
			}
		}
		if(current != null && current.sym != 0)
			ret.append(current.sym);
		return ret.toString();
	}
	
	String encoding(char c)
	{
		//find the right node in the tree
		Symbol cs = getSymbolForChar(c,0,alphabet.length-1);
		//reconstruct its encoding by following the path up to the root
		Symbol prev = cs;
		Symbol current = cs.parent;
		StringBuilder enc = new StringBuilder();
		while(current != null)
		{
			String add = current.left == prev ? "0" : "1";
			enc.insert(0, add);
			prev = current;
			current = current.parent;
		}
		return enc.toString();
	}
	
	/*
	Encode the string but don't add the tree
	 */
	String encode(String s)
	{
		StringBuilder ret = new StringBuilder();
		for(int i = 0; i < s.length(); i++)
		{
			ret.append(encoding(s.charAt(i)));
		}
		return ret.toString();
	}

	private void treeRep(Symbol node, StringBuilder ac)
	{
		if(node.left == null)
		{
			//this is a leaf
			ac.append(node.sym);
			return;
		}
		ac.append(META_OPEN);
		treeRep(node.left,ac);
		treeRep(node.right,ac);
//		ac.append(TREE_POP);
	}
	
	String tree()
	{
		StringBuilder sb = new StringBuilder();
		treeRep(root,sb);
		return sb.toString();
	}
	
	void show()
	{
		System.out.println(tree().replace(META_OPEN,META_OPEN_RENDER));
	}
	
	/*
	REPRESENTATION:
	tree: metasymbol-open is all zeroes, all else is ascii encoding of the char
	msg: compress every 8 bits of message to a single byte then add that to the array
	 */
	byte[] encodedMsgToRep(String msg)
	{
		Stack<Integer> daughtersLeft = new Stack();
		int i = 1;
		ArrayList<Byte> ra = new ArrayList(msg.length());
		ra.add((byte)0);
		daughtersLeft.push(2);
		while(!daughtersLeft.empty())
		{
			int dlAtLevelL = daughtersLeft.pop();
			dlAtLevelL--;
			if(dlAtLevelL != 0)
				daughtersLeft.push(dlAtLevelL);
			if(msg.charAt(i) == META_OPEN)
			{
				daughtersLeft.push(2);
				ra.add((byte)0);
			}
			else
			{
				ra.add((byte)msg.charAt(i));
			}
			i++;
		}
		//tree is now stored
		
		byte buffer = 0;
		byte shift = 0;
		for(;i < msg.length();i++)
		{
			//push this 1 or 0 onto the buffer (low bits first) then increment shift. if that fills it (shift = 8), add it to the array and reset the buffer
			//remember that low bits first means these vectors are little endian
			byte add = msg.charAt(i) == '0' ? (byte)0 : (byte)1;
			buffer |= add << shift;
			shift++;
			if(shift == 8)
			{
				//add buffer to array and reset
				ra.add(buffer);
				buffer ^= buffer;
				shift ^= shift;
			}
		}
		if(shift != 0)
			ra.add(buffer);//add the last buffer if we didn't already fill it
		
		byte[] ret = new byte[ra.size()];
		for(int j = 0; j < ret.length; j++)
			ret[j] = ra.get(j);
		return ret;
	}
	
	private void writeToFile(String filename, String emsg)
	{
		String encodedMsg = tree() + emsg;//assume the message is already encoded
		FileOutputStream fos = null;
		try
		{
			fos = new FileOutputStream(filename);
			byte[] encMsgBA = encodedMsgToRep(encodedMsg);
			fos.write(encMsgBA);
			fos.close();
		} catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	void readEncodedFile(String filename, String outfile)
	{
		//first read the tree and build it manually
		byte[] file = null;
		try
		{
			file = Files.readAllBytes(Paths.get(filename));
		}
		catch(Exception ignored){}
		
		//now we have the bytes, read the tree
		Stack<Integer> daughtersLeft = new Stack();
		Stack<Integer> indexInSig = new Stack();
		int i = 1;
		daughtersLeft.push(2);
		indexInSig.push(0);
//		int maxIndexInSig = 0;
		ArrayList<Symbol> sig = new ArrayList();
		root = new Symbol(null,null,0);
		sig.add(root);
		int countRegularSymbols = 0;
		while(!daughtersLeft.empty())
		{
			int dlAtLevelL = daughtersLeft.pop();
			dlAtLevelL--;
			Symbol current = new Symbol();
			//set up the parent and left or right pointers
			if(dlAtLevelL == 0)//set the right daughter
				sig.get(indexInSig.peek()).right = current;
			else
				sig.get(indexInSig.peek()).left = current;
			current.parent = sig.get(indexInSig.peek());
			if(dlAtLevelL != 0)
				daughtersLeft.push(dlAtLevelL);
			else
				indexInSig.pop();
			if(file[i] == 0)
			{
				//metasymbol-open
				daughtersLeft.push(2);
				indexInSig.push(sig.size());
			}
			else
			{
				//regular symbol
				current.sym = (char)file[i];
				countRegularSymbols++;
			}
			
			sig.add(current);
			i++;
		}
		
		//now we have to set up alphabet
		initAlphabet(sig,countRegularSymbols);
		//now we can read the message
		if(outfile != null)
		{
			PrintWriter pw = null;
			try
			{
				pw = new PrintWriter(new File(outfile));
			}
			catch (Exception ignored){}
			//first things first: convert from byte array to a bunch of chars
			StringBuilder emsb = new StringBuilder();
			for(; i < file.length; i++)
			{
				byte b = file[i];
				for(byte shift = 0; shift < 8; shift++)
				{
					int here = (b & (1 << shift)) >> shift;
					char add = here == 0 ? '0' : '1';
					emsb.append(add);
				}
			}
			String em = emsb.toString();
			//now we have the message as a string, just have to decode
			pw.print(decode(em));
			pw.close();
		}
	}
	
	ArrayList<Symbol> getHistogramFromString(String s)
	{
		ArrayList<Symbol> sig = new ArrayList();
		HashMap<Character,Symbol> alphaset = new HashMap();
		for(int i = 0; i < s.length(); i++)
		{
			char current = s.charAt(i);
			if(alphaset.containsKey(current))
				alphaset.get(current).frequency++;
			else
			{
				//character not in sigma, add it and initialize the count to 1
				Symbol newSymbol = new Symbol(current,1);
				sig.add(newSymbol);
				alphaset.put(current,newSymbol);
			}
		}
		return sig;
	}
	
	String readUnencodedFile(String filename)
	{
		String file = "";
		try
		{
			BufferedReader br = new BufferedReader(new FileReader(filename));
			StringBuilder in = new StringBuilder();
			String next = br.readLine();
			while(next != null)
			{
				in.append(next);
				next = br.readLine();
				if(next != null)
					in.append("\n");
			}
			file = in.toString();
		}
		catch (Exception ignored){}
		
		//now we have the whole file in 1 string, we have to build the tree from it. to do that we have to get the histogram
		ArrayList<Symbol> alpha = getHistogramFromString(file);
		//now we have the histogram, build the tree
		runHF(alpha);
		
		return encode(file);
	}
	
	void encodeFileAndOutputToFile(String infile, String outfile)
	{
		String encodedFile = readUnencodedFile(infile);
		writeToFile(outfile,encodedFile);
	}
}
