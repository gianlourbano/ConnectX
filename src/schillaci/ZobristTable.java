package schillaci;

import java.util.Random;
import java.util.concurrent.TimeoutException;
import connectx.CXBoard;

public class ZobristTable {
    private long[][][] zobristTable; // 3D array to store random long values for each cell state and player

    public ZobristTable(int M, int N) {
        zobristTable = new long[M][N][2]; // initialize the hash table with random values for each cell state and player
        Random rand = new Random(System.currentTimeMillis()); // create a new random number generator
        for (int i = 0; i < M; i++)
            for (int j = 0; j < N; j++)
                for (int k = 0; k < 2; k++)
                    zobristTable[i][j][k] = rand.nextLong(); // assign a random long value to each cell state and player
    }

    public long computeHash(CXBoard board) {
        long hash = 0; // initialize the hash value to zero
        for (int i = 0; i < board.M; i++)
            for (int j = 0; j < board.N; j++)
                if (board.cellState(i, j).ordinal() != 2) // check if the cell is empty
                    hash ^= zobristTable[i][j][board.cellState(i, j).ordinal()]; // XOR the corresponding random value with the current hash value
        return hash; // return the final hash value
    }
}

// Si può migliorare: l'hash non va ricalcolato ogni volta, basta fare uno xor della mossa che si sta per fare con l'hash corrente e rifarlo quando si torna indietro
// a xor b xor b = a