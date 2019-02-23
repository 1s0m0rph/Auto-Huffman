# Auto-Huffman
Tool for creating huffman-encoded plaintext files


I call the encoded files .huf. They record the entire tree in (more or less) plain ascii, then the entire message byte-by-byte in plain huffman code.

The tree format is a little unusual. A NULL character (ascii-00) represents a metasymbol; that is, an internal node in the tree. Since it's a strict binary tree we can just record the tree using a sort of modified L-system (specifically, a modification on the L-system used to create turtle walks; more specifically, a modification on one which includes brackets as stach pushes and pops in order to create tree structures that eliminates the close bracket).

Say I have a huffman tree:

```
         .
        / \
       /   \
      .     .
     / \   / \
    .   c d   e
   / \
  a   b
```

We can write this tree in a flat format as: `|||abc|de`

where the "|" symbol is a stand-in for the real encoding (NULL). We traverse the tree, adding a NULL when there's an internal node or the letter when there's a leaf, then simply recurse on the left and right daughter until we're done.
