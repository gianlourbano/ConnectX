package connectx.schillaci;

import java.util.Arrays;
public class TranspositionTableDEEP {
    // Define constants for the three types of hash table entries
    public static final int EXACT = 0;
    public static final int LOWER_BOUND = 1;
    public static final int UPPER_BOUND = 2;

    // Define a nested class to represent a hash table entry
    public static class HashNode {
        protected int value; // The value associated with the position
        protected int depth; // The depth at which the position was evaluated
        protected int type; // The type of the hash table entry

        public HashNode(int value, int depth, int type) {
            this.value = value;
            this.depth = depth;
            this.type = type;
        }
    }
// 
    private final int[] table;
    private final byte[] depths;
    private final byte[] flags; // The byte array that stores the hash table entries
    private final int size; // The size of the hash table
    private final int mask; // A bitmask used to compute the index of each entry

    public TranspositionTableDEEP(int size) {
        this.size = size;
        this.mask = size - 1;
        this.table = new int[this.size];
        this.depths = new byte[this.size];
        this.flags = new byte[this.size];
        // Initialize all entries in the hash table to -1
        Arrays.fill(table, -1);
        Arrays.fill(depths, (byte) -1);
        Arrays.fill(flags, (byte) -1);
    }

    // Search for a hash table entry with a given hash key and depth
    public int searchHashNode(long hashKey, int depth, int alpha, int beta) {
        int index = (int) (hashKey & mask);
        int value = Integer.MAX_VALUE;

        if(index >= this.size) return value;

        if (table[index] != -1) {
            // If an entry exists at the computed index, create a HashNode object to represent it
            HashNode current = new HashNode(table[index], depths[index], flags[index]);

            // If the depth of the existing entry is greater than the input depth, return the value of the entry
            if (current.depth > depth) {
                if (current.type == 0) {
                    value = current.value;
                } else if (current.type == 1 && current.value <= alpha) {
                    value = alpha;
                } else if (current.type == 2 && current.value >= beta) {
                    value = beta;
                }
            }
        }

        return value;
    }

    // Store a hash table entry with a given hash key, value, depth, and type
    public void storeHashNode(long hashKey, int value, int depth, int type) {
        int index = (int) (hashKey & mask);

        if(index >= this.size) return;

        if (table[index] == -1) {
            // If the entry at the computed index is empty, set its value, depth, and type to the input values
            table[index] = value;
            depths[index] = (byte) depth;
            flags[index] = (byte) type;
        } else {
            // If the input depth is greater than or equal to the depth of the existing entry, update the entry
            if (depth >= depths[index]) {
                table[index] = value;
                depths[index] = (byte) depth;
                flags[index] = (byte) type;
            }
        }
    }
}