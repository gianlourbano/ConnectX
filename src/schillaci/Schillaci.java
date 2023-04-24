package schillaci;

import java.util.Random;
import java.util.concurrent.TimeoutException;

import connectx.CXBoard;
import connectx.CXGameState;
import connectx.CXPlayer;

public class Schillaci implements CXPlayer {
    private int timeout_in_secs;

    private Random rand;
    private CXGameState myWin;
    private CXGameState yourWin;

    private long START;
    private int winCutoff = 500000;

    private boolean searchCutoff = false;

    public void initPlayer(int M, int N, int X, boolean first, int timeout_in_secs) {
        myWin = first ? CXGameState.WINP1 : CXGameState.WINP2;
        yourWin = first ? CXGameState.WINP2 : CXGameState.WINP1;
        this.timeout_in_secs = timeout_in_secs;

        rand = new Random(System.currentTimeMillis());
    }

    public int selectColumn(CXBoard B) {
        START = System.currentTimeMillis();

        Integer[] L = B.getAvailableColumns();

        int max_score = Integer.MIN_VALUE;
        int best_move = -1;

        int score;

        for (int col : L) {
            B.markColumn(col);
            long search_time_limit = ((timeout_in_secs - 1) / L.length);
            try {
                score = IterativeDeepening(B, search_time_limit);
            } catch (TimeoutException e) {
                return best_move;
            }

            B.unmarkColumn();
            if (score >= winCutoff)
                return col;

            if (score > max_score) {
                max_score = score;
                best_move = col;
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

            if (searchResult >= winCutoff) {
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
        Integer[] L = B.getAvailableColumns();
        boolean isMax = B.currentPlayer() == 0;

        int score = B.gameState() == myWin ? winCutoff : B.gameState() == yourWin ? -winCutoff : 0;

        long elapsedTime = System.currentTimeMillis() - startTime;

        if (elapsedTime >= timeLimit) {
            searchCutoff = true;
            return score;
        }

        if (depth == 0 || B.gameState() != CXGameState.OPEN || score >= winCutoff || score <= -winCutoff) {
            return score;
        }

        if (isMax) {
            for (int col : L) {
                B.markColumn(col);
                alpha = Math.max(alpha, search(B, depth - 1, alpha, beta, startTime, timeLimit));
                B.unmarkColumn();

                if (beta <= alpha)
                    break;
            }

            return alpha;
        } else {
            for (int col : L) {
                B.markColumn(col);
                beta = Math.min(beta, search(B, depth - 1, alpha, beta, startTime, timeLimit));
                B.unmarkColumn();

                if (beta <= alpha)
                    break;
            }

            return beta;
        }

    }

    private void checktime() throws TimeoutException {
        if ((System.currentTimeMillis() - START) / 1000.0 >= timeout_in_secs * (55.0 / 100.0)) {
            throw new TimeoutException();
        }
    }

    public String playerName() {
        return "Schillaci";
    }
}
