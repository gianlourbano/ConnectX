package connectx.schillaci;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.TimeoutException;

import connectx.CXBoard;
import connectx.CXGameState;
import connectx.CXPlayer;

public class Schillaci_A implements CXPlayer {
    private int timeout_in_secs;

    private int M, N, X;
    private boolean first;

    private long START;
    private int winCutoff = 500000;

    private boolean searchCutoff = false;
    private Evaluator evaluator;
    private ZobristTable zobristTable;
    private TranspositionTableDEEP transpositionTable;

    // data
    private long numNodes = 0;
    private long numNodesEvaluated = 0;
    private long numNodesPruned = 0;
    private long numNodesReused = 0;
    private String playerNameString = "Schillaci_L1";

    public void initPlayer(int M, int N, int X, boolean first, int timeout_in_secs) {
        this.timeout_in_secs = timeout_in_secs;
        this.first = first;

        evaluator = new Evaluator(M, N, X);
        transpositionTable = new TranspositionTableDEEP(100 * 1024 * 1024);
        zobristTable = new ZobristTable(M, N);
        this.M = M;
        this.N = N;
        this.X = X;

        numNodes = 0;
        numNodesEvaluated = 0;
        numNodesPruned = 0;
        numNodesReused = 0;
    }

    // Log data
    public void Exit(CXBoard B) {
        File data = new File("data_" + playerNameString + ".csv");
        if(!data.exists() && !data.isDirectory()) {
            FileWriter fw;
            try {
                fw = new FileWriter("data_" + playerNameString + ".csv", true);
                fw.write("M,N,X,First,TotalMoves,TotalNodes,TotalNodesEvaluated,TotalNodesPruned,TotalNodesReused\n");
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
        return B.numOfMarkedCells() < 2;
    }

    public int selectColumn(CXBoard B) {
        START = System.currentTimeMillis();

        Integer[] L = B.getAvailableColumns();
        // order moves based on how for they are from the center
        Arrays.sort(L, new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return Math.abs(o1 - N / 2) - Math.abs(o2 - N / 2);
            }
        });

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
        boolean isMax = B.currentPlayer() == 0;

        int score = evaluator.eval(B);

        long elapsedTime = System.currentTimeMillis() - startTime;

        if (elapsedTime >= timeLimit) {
            searchCutoff = true;
            return score;
        }

        if (depth == 0 || B.gameState() != CXGameState.OPEN || score >= winCutoff || score <= -winCutoff) {
            numNodesEvaluated++;
            return score;
        }

        if (isMax) {
            for (int col : L) {
                B.markColumn(col);
                alpha = Math.max(alpha, search(B, depth - 1, alpha, beta, startTime, timeLimit));
                B.unmarkColumn();

                if (beta <= alpha) {
                    transpositionTable.storeHashNode(hash, alpha, depth, TranspositionTableDEEP.LOWER_BOUND);
                    numNodesPruned++;
                    break;
                }
            }

            transpositionTable.storeHashNode(hash, alpha, depth, TranspositionTableDEEP.EXACT);
            return alpha;
        } else {
            for (int col : L) {
                B.markColumn(col);
                beta = Math.min(beta, search(B, depth - 1, alpha, beta, startTime, timeLimit));
                B.unmarkColumn();

                if (beta <= alpha) {
                    transpositionTable.storeHashNode(hash, beta, depth, TranspositionTableDEEP.UPPER_BOUND);
                    numNodesPruned++;
                    break;
                }
            }

            transpositionTable.storeHashNode(hash, beta, depth, TranspositionTableDEEP.EXACT);
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