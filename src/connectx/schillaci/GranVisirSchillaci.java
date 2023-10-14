package connectx.schillaci;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.TimeoutException;

import connectx.CXBoard;
import connectx.CXGameState;
import connectx.CXPlayer;
import connectx.schillaci.TableTWOBIG1.TTEntry;
import connectx.schillaci.TableTWOBIG1.TTResult;

public class GranVisirSchillaci implements CXPlayer {
    
    // initialization parameters
    private int M, N, X;
    private boolean first;
    private int timeout_in_secs;

    // time
    private long START;

    // search parameters
    private int winCutoff = 500000;
    private boolean searchCutoff = false;

    // search data structures and functions
    private Evaluator evaluator;
    private ZobristTable zobristTable;
    private HashMap<Long, TTEntry> transpositionTable;

    // data logging
    private long inspectedNodes = 0;
    private long numNodes = 0;
    private long numNodesEvaluated = 0;
    private long numNodesPruned = 0;
    private long numNodesReused = 0;
    private String playerNameString = "GranVisirSchillaci";

    // random number generator
    private Random rand;

    // game states
    CXGameState myWin, yourWin;

    // constants
    private static final float RELAXATION = 90.0f;

    /**
     * Initializes the player.
     * 
     * @param M               the number of rows of the board
     * @param N               the number of columns of the board
     * @param X               the number of adjacent cells to win
     * 
     * @param first           true if the player must make the first move, false otherwise
     * @param timeout_in_secs the time available for the choice of each move
     * 
     */
    public void initPlayer(int M, int N, int X, boolean first, int timeout_in_secs) {
        this.timeout_in_secs = timeout_in_secs;
        this.first = first;

        evaluator = new Evaluator(M, N, X);
        zobristTable = new ZobristTable(M, N);
        this.M = M;
        this.N = N;
        this.X = X;

        numNodes = 0;
        numNodesEvaluated = 0;
        numNodesPruned = 0;
        numNodesReused = 0;

        myWin = first ? CXGameState.WINP1 : CXGameState.WINP2;
        yourWin = first ? CXGameState.WINP2 : CXGameState.WINP1;

        transpositionTable = new HashMap<Long, TTEntry>(1024 * 1024 * 8);
        rand = new Random(System.currentTimeMillis());
    }

    /**
     * Introduced for data logging purposes.
     * 
     * @param B the final board of a game.
     */
    public void Exit(CXBoard B) {
        File data = new File("data_" + playerNameString + ".csv");
        if (!data.exists() && !data.isDirectory()) {
            FileWriter fw;
            try {
                fw = new FileWriter("data_" + playerNameString + ".csv", true);
                fw.write(
                        "M,N,X,First,TotalMoves,TotalNodes,TotalNodesEvaluated,TotalNodesPruned,TotalNodesReused\n");
                fw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

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
        return B.numOfMarkedCells() < 1;
    }

    /**
     * Selects the best column to play, given the current board configuration.
     * For every available move, it performs an iterative deepening search with a
     * time limit. If the time is running out, it returns the best move found so far.
     * 
     * @param B the board
     * @return the column to play
     */
    public int selectColumn(CXBoard B) {
        START = System.currentTimeMillis();

        Integer[] L = B.getAvailableColumns();

        if (isFirstMove(B))
            return this.N / 2;

        int max_score = first ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        int best_move = L[rand.nextInt(L.length)];

        int score;

        for (int col : L) {
            B.markColumn(col);
            long search_time_limit = ((timeout_in_secs - 1) / L.length) * 30;
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

    /**
     * Performs an iterative deepening search with a time limit.
     * @param B the board
     * @param search_time_limit the time limit for the search
     * @return the score of the best move
     * @throws TimeoutException if the time is running out.
     */
    private int IterativeDeepening(CXBoard B, long search_time_limit) throws TimeoutException {
        checktime();
        
        int depth = 1;
        int score = 0;
        searchCutoff = false;
        
        long endTime = System.currentTimeMillis() + search_time_limit;
        
        while (true) {
            inspectedNodes = 0;
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

    /**
     * Performs a minmax search with alpha-beta pruning, using a transposition table with the TWOBIG1 replacement method.
     * 
     * @param B the board
     * @param depth the depth of the search
     * @param alpha the alpha value
     * @param beta the beta value
     * @param startTime the time when the search started
     * @param timeLimit the time limit for the search
     * @return the score of the best move
     * @throws TimeoutException if the time is running out.
     */

    private int search(CXBoard B, int depth, int alpha, int beta, long startTime, long timeLimit)
            throws TimeoutException {
        numNodes++;
        final long prevNodes = inspectedNodes++;

        Integer[] L = B.getAvailableColumns();
        boolean isMax = B.currentPlayer() == 0;

        int score = evaluator.eval(B);

        long elapsedTime = System.currentTimeMillis() - startTime;

        if (elapsedTime >= timeLimit) {
            searchCutoff = true;
            return score;
        }

        CXGameState state = B.gameState();
        if (depth == 0 || state != CXGameState.OPEN || score >= winCutoff || score <= -winCutoff) {
            numNodesEvaluated++;
            return score;
        }

        long hash = zobristTable.computeHash(B);
        final TTEntry cachedEntry = transpositionTable.get(hash);
        int bestMove = -1, cachedMove = -1;
        if (cachedEntry != null) {
            final TTResult cachedResult = cachedEntry.pickResult(B, depth);
            if (cachedResult != null) {
                if (depth <= cachedResult.depth) {
                    switch (cachedResult.flag) {
                        case EXACT:
                            numNodesReused++;
                            return cachedResult.score;
                        case UPPER_BOUND:
                            beta = Math.min(beta, cachedResult.score);
                            break;
                        case LOWER_BOUND:
                            alpha = Math.max(alpha, cachedResult.score);
                            break;
                        default:
                            throw new InternalError("Unexpected result type " + cachedResult.flag);
                    }
                    if (beta <= alpha) {
                        numNodesPruned++;
                        return isMax ? alpha : beta;
                    }
                }

                bestMove = cachedResult.bestMove;
                cachedMove = bestMove;

            }
        }

        Integer v = null;

        if (bestMove != -1) {
            B.markColumn(bestMove);
            v = search(B, depth - 1, alpha, beta, startTime, timeLimit);
            B.unmarkColumn();

            if (isMax) {
                if (v.compareTo(beta) >= 0) {
                    storeResult(hash, cachedEntry,
                            new TTResult(bestMove, v, TTResult.type.UPPER_BOUND, depth, inspectedNodes - prevNodes));
                    numNodesPruned++;
                    return v;
                }
                alpha = Math.max(alpha, v);
            } else {
                if (v.compareTo(alpha) <= 0) {
                    storeResult(hash, cachedEntry,
                            new TTResult(bestMove, v, TTResult.type.LOWER_BOUND, depth, inspectedNodes - prevNodes));
                    numNodesPruned++;
                    return v;
                }
                beta = Math.min(beta, v);
            }
        }

        if (isMax) {
            for (int col : L) {
                if (col != cachedMove) {
                    B.markColumn(col);
                    int newV = search(B, depth - 1, alpha, beta, startTime, timeLimit);

                    if (v == null || newV > v) {
                        v = newV;
                        bestMove = col;
                    }
                    B.unmarkColumn();

                    if (v.compareTo(beta) >= 0) {
                        storeResult(hash, cachedEntry,
                                new TTResult(bestMove, v, TTResult.type.LOWER_BOUND, depth, inspectedNodes - prevNodes));
                        numNodesPruned++;
                        return v;
                    }

                    alpha = Math.max(alpha, v);
                }

            }
        } else {
            for (int col : L) {
                if (col != cachedMove) {
                    B.markColumn(col);
                    int newV = search(B, depth - 1, alpha, beta, startTime, timeLimit);

                    if (v == null || newV < v) {
                        v = newV;
                        bestMove = col;
                    }
                    B.unmarkColumn();

                    if (v.compareTo(alpha) <= 0) {
                        storeResult(hash, cachedEntry,
                                new TTResult(bestMove, v, TTResult.type.UPPER_BOUND, depth, inspectedNodes - prevNodes));
                        numNodesPruned++;
                        return v;
                    }

                    beta = Math.min(beta, v);
                }

            }
        }

        storeResult(hash, cachedEntry,
                new TTResult(bestMove, v, TTResult.type.EXACT, depth, inspectedNodes - prevNodes));
        return v;
    }

    private void storeResult(long hash, TTEntry cachedEntry, TTResult r) throws TimeoutException {
        if (cachedEntry == null) {
            transpositionTable.put(hash, new TTEntry(r));
        } else {
            cachedEntry.add(r);
        }
        checktime();
    }

    private void checktime() throws TimeoutException {
        if ((System.currentTimeMillis() - START) / 1000.0 >= timeout_in_secs * (RELAXATION / 100.0)) {
            throw new TimeoutException();
        }
    }

    public String playerName() {
        return playerNameString;
    }
}