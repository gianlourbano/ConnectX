package schillaci;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeoutException;

import connectx.CXBoard;
import connectx.CXGameState;
import connectx.CXPlayer;

public class Schillaci_A implements CXPlayer {
    private int timeout_in_secs;

    private int moveNumber;

    private Random rand;

    private int M, N, X;
    private boolean first;

    private long START;
    private int winCutoff = 500000;

    private boolean searchCutoff = false;
    private Evaluator evaluator;
    private ZobristTable zobristTable;
    private TranspositionTable transpositionTable;
    private CXGameState myWin, yourWin;

    // data
    private int numNodes = 0;
    private int numNodesEvaluated = 0;
    private int numNodesPruned = 0;
    private int numNodesReused = 0;
    private String playerNameString = "Schillaci";
    
    // killer moves
    private int[][] killerMoves;

    public Schillaci_A() {
        moveNumber = 0;
    }

    public void initPlayer(int M, int N, int X, boolean first, int timeout_in_secs) {
        this.timeout_in_secs = timeout_in_secs;
        this.first = first;

        rand = new Random(System.currentTimeMillis());
        evaluator = new Evaluator(M, N, X);
        transpositionTable = new TranspositionTable(100 * 1024 * 1024 * 4);
        zobristTable = new ZobristTable(M, N);
        this.M = M;
        this.N = N;
        this.X = X;

        myWin = first ? CXGameState.WINP1 : CXGameState.WINP2;
        yourWin = first ? CXGameState.WINP2 : CXGameState.WINP1;

        numNodes = 0;
        numNodesEvaluated = 0;
        numNodesPruned = 0;
        numNodesReused = 0;

        // Initialize killer moves array
        killerMoves = new int[M][2];
        for (int i = 0; i < M; i++) {
            Arrays.fill(killerMoves[i], -1);
        }
    }

    // Log data
    public void Exit(CXBoard B) {
        FileWriter fw;
        int totMoves = B.numOfMarkedCells();
        try {
            fw = new FileWriter("data_" + playerNameString + ".csv", true);
            fw.write(M + "," + N + "," + X + "," + first + "," + totMoves + "," + numNodes + "," + numNodesEvaluated
                    + "," + numNodesPruned + "," + numNodesReused + "\n");
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private boolean isFirstMove(CXBoard B) {
        return B.numOfMarkedCells() < 2;
    }

    private void shuffle(Integer[] L) {
        for (int i = 0; i < L.length; i++) {
            int randomIndexToSwap = rand.nextInt(L.length);
            int temp = L[randomIndexToSwap];
            L[randomIndexToSwap] = L[i];
            L[i] = temp;
        }
    }

    public int selectColumn(CXBoard B) {
        moveNumber++;
        
        START = System.currentTimeMillis();

        Integer[] L = B.getAvailableColumns();
        shuffle(L);

        if (isFirstMove(B))
            return this.N / 2;

        int max_score = first ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        int best_move = -1;

        int score;

        for (int col : L) {
            B.markColumn(col);
            long search_time_limit = ((timeout_in_secs - 1) / L.length) * 40;
            try {
                score = IterativeDeepening(B, search_time_limit);
            } catch (TimeoutException e) {
                return best_move;
            }

            B.unmarkColumn();

            if (first) {
                if (score >= winCutoff)
                    return col;

                if (score > max_score) {
                    max_score = score;
                    best_move = col;
                }
            } else {
                if (score <= -winCutoff)
                    return col;

                if (score < max_score) {
                    max_score = score;
                    best_move = col;
                }
            }
        }

        return best_move;
    }

    private int IterativeDeepening(CXBoard B, long search_time_limit) throws TimeoutException {
        checktime();

        int depth = 1;
        int score = 0;
        searchCutoff = false;

        long endTime = System.currentTimeMillis() + search_time_limit;

        while (true) {
            // checktime();

            long start = System.currentTimeMillis();
            if (start >= endTime)
                break;

            int searchResult = search(B, depth, Integer.MIN_VALUE, Integer.MAX_VALUE, start, endTime - start);

            if (Math.abs(searchResult) >= winCutoff) {
                return searchResult;
            }

            if (!searchCutoff) {
                score = searchResult;
            }

            depth++;
        }
        return score;
    }

    private int search(CXBoard B, int depth, int alpha, int beta, long startTime, long timeLimit) {
        numNodes++;

        long hash = zobristTable.computeHash(B);
        int value = transpositionTable.searchHashNode(hash, depth, alpha, beta);
        if (value != Integer.MAX_VALUE) {
            numNodesReused++;
            return value;
        }

        Integer[] L = B.getAvailableColumns();
        shuffle(L);
        boolean isMax = B.currentPlayer() == 0;

        int score = evaluator.eval(B);

        long elapsedTime = System.currentTimeMillis() - startTime;

        if (elapsedTime >= timeLimit) {
            searchCutoff = true;
            return score;
        }

        if (depth == 0 || B.gameState() != CXGameState.OPEN || score >= winCutoff || score <= -winCutoff) {
            numNodesEvaluated++;
            transpositionTable.storeHashNode(hash, score, depth, TranspositionTable.EXACT);
            return score;
        }

        if (isMax) {
            for (int col : L) {
                if (col != killerMoves[moveNumber][0] && col != killerMoves[moveNumber][1]) {
                    B.markColumn(col);
                    alpha = Math.max(alpha, search(B, depth - 1, alpha, beta, startTime, timeLimit));
                    B.unmarkColumn();

                    if (beta <= alpha) {
                        numNodesPruned++;
                        break;
                    }
                }
            }

            transpositionTable.storeHashNode(hash, alpha, depth, TranspositionTable.LOWER_BOUND);
            return alpha;
        } else {
            for (int col : L) {
                if (col != killerMoves[moveNumber][0] && col != killerMoves[moveNumber][1]) {
                    B.markColumn(col);
                    beta = Math.min(beta, search(B, depth - 1, alpha, beta, startTime, timeLimit));
                    B.unmarkColumn();

                    if (beta <= alpha) {
                        numNodesPruned++;
                        break;
                    }
                }
            }

            transpositionTable.storeHashNode(hash, beta, depth, TranspositionTable.UPPER_BOUND);
            return beta;
        }
    }

    private void checktime() throws TimeoutException {
        if ((System.currentTimeMillis() - START) / 1000.0 >= timeout_in_secs * (99.0 / 100.0)) {
            throw new TimeoutException();
        }
    }

    public String playerName() {
        return playerNameString;
    }
}
