package schillaci;

import java.util.Arrays;
public class TranspositionTable implements TT {
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

    private final byte[] table; // The byte array that stores the hash table entries
    private final int size; // The size of the hash table
    private final int mask; // A bitmask used to compute the index of each entry

    public TranspositionTable(int size) {
        this.size = size;
        this.mask = size - 1;
        this.table = new byte[this.size];
        // Initialize all entries in the hash table to -1
        Arrays.fill(table, (byte) -1);
    }

    // Get the value associated with a hash table entry at a given index
    private int getValue(int index) {
        return (table[index] & 0xFF) | ((table[index + 1] & 0xFF) << 8) | ((table[index + 2] & 0xFF) << 16)
                | ((table[index + 3] & 0xFF) << 24);
    }

    // Set the value associated with a hash table entry at a given index
    private void setValue(int index, int value) {
        table[index] = (byte) (value & 0xFF);
        table[index + 1] = (byte) ((value >> 8) & 0xFF);
        table[index + 2] = (byte) ((value >> 16) & 0xFF);
        table[index + 3] = (byte) ((value >> 24) & 0xFF);
    }

    // Get the depth associated with a hash table entry at a given index
    private int getDepth(int index) {
        return table[index + 4];
    }

    // Set the depth associated with a hash table entry at a given index
    private void setDepth(int index, int depth) {
        table[index + 4] = (byte) depth;
    }

    // Get the type associated with a hash table entry at a given index
    private int getType(int index) {
        return table[index + 5];
    }

    // Set the type associated with a hash table entry at a given index
    private void setType(int index, int type) {
        table[index + 5] = (byte) type;
    }

    // Search for a hash table entry with a given hash key and depth
    public int searchHashNode(long hashKey, int depth, int alpha, int beta) {
        int index = (int) (hashKey & mask);
        int value = Integer.MAX_VALUE;
        int type = 0;
        int storedDepth = 0;

        if (table[index] != -1) {
            // If an entry exists at the computed index, create a HashNode object to represent it
            HashNode current = new HashNode(getValue(index), getDepth(index), getType(index));

            // If the depth of the existing entry is greater than the input depth, return the value of the entry
            if (current.depth > depth) {
                if (current.type == 0) {
                    value = current.value;
                } else if (current.type == 1 && current.value <= alpha) {
                    value = alpha;
                } else if (current.type == 2 && current.value >= beta) {
                    value = beta;
                } else {
                    storedDepth = current.depth;
                    type = current.type;
                }
            }
        }

        return value;
    }

    // Store a hash table entry with a given hash key, value, depth, and type
    public void storeHashNode(long hashKey, int value, int depth, int type) {
        int index = (int) (hashKey & mask);

        if (table[index] == -1) {
            // If the entry at the computed index is empty, set its value, depth, and type to the input values
            setValue(index, value);
            setDepth(index, depth);
            setType(index, type);
        } else {
            // If the entry at the computed index is not empty, create a HashNode object to represent it
            HashNode current = new HashNode(getValue(index), getDepth(index), getType(index));

            // If the input depth is greater than or equal to the depth of the existing entry, update the entry
            if (depth >= current.depth) {
                setValue(index, value);
                setDepth(index, depth);
                setType(index, type);
            }
        }
    }
}