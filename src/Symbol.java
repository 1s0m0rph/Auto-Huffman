import java.util.Comparator;

public class Symbol implements Comparator<Symbol>
{
	/*
	Contains the abstraction for symbols in the alphabet. Consists either of a single character (i.e. a leaf node) or a metasymbol with two daughters.
	 */
	
	char sym = 0;//0 for metasymbols
	Symbol left = null;
	Symbol right = null;
	Symbol parent = null;
	int frequency = 0;
	
	public Symbol(){}
	
	/*
	For regular leaf symbols
	 */
	public Symbol(char _sym, int freq)
	{
		sym = _sym;
		frequency = freq;
	}
	
	/*
	For regular leaf symbols
	 */
	public Symbol(char _sym)
	{
		sym = _sym;
	}
	
	/*
	For metasymbols
	 */
	public Symbol(Symbol left, Symbol right, int freq)
	{
		this.left = left;
		this.right = right;
		frequency = freq;
	}
	
	public int compare(Symbol a, Symbol b)
	{
		return (int)a.sym - (int)b.sym;
	}
}