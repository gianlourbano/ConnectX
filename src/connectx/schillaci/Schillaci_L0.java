    package connectx.schillaci;

    import java.io.File;
    import java.io.FileWriter;
    import java.io.IOException;
    import java.util.Random;
    import java.util.concurrent.TimeoutException;

    import connectx.CXBoard;
    import connectx.CXGameState;
    import connectx.CXPlayer;

    public class Schillaci_L0 implements CXPlayer {
        private int timeout_in_secs;

        private Random rand;

        private boolean first;

        private long START;
        private int winCutoff = 500000;

        private boolean searchCutoff = false;
        private String playerNameString = "Schillaci_L0";
        private int numNodes, numNodesEvaluated, numNodesPruned, numNodesReused;
        private int M, N, X;

        public void initPlayer(int M, int N, int X, boolean first, int timeout_in_secs) {
            this.timeout_in_secs = timeout_in_secs;
            this.first = first;

            rand = new Random(System.currentTimeMillis());
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

        private void shuffle(Integer[] L) {
            for (int i = 0; i < L.length; i++) {
                int randomIndexToSwap = rand.nextInt(L.length);
                int temp = L[randomIndexToSwap];
                L[randomIndexToSwap] = L[i];
                L[i] = temp;
            }
        }

        public int selectColumn(CXBoard B) {
            START = System.currentTimeMillis();

            Integer[] L = B.getAvailableColumns();
            shuffle(L);

            int max_score = first ? Integer.MIN_VALUE : Integer.MAX_VALUE;
            int best_move = -1;

            int score;

            for (int col : L) {
                B.markColumn(col);
                long search_time_limit = ((timeout_in_secs - 1) / L.length) * 10;
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

            // System.out.println("Nodes: " + totNodes);
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
            numNodes++;
            Integer[] L = B.getAvailableColumns();
            boolean isMax = B.currentPlayer() == 0;

            int score = eval(B);

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
                        numNodesPruned++;
                        break;
                    }
                }

                return alpha;
            } else {
                for (int col : L) {
                    B.markColumn(col);
                    beta = Math.min(beta, search(B, depth - 1, alpha, beta, startTime, timeLimit));
                    B.unmarkColumn();

                    if (beta <= alpha) {
                        numNodesPruned++;
                        break;
                    }
                }

                return beta;
            }

        }

        private int eval(CXBoard B) {
            return B.gameState() == CXGameState.WINP1 ? winCutoff : B.gameState() == CXGameState.WINP2 ? -winCutoff : 0;
        }

        private void checktime() throws TimeoutException {
            if ((System.currentTimeMillis() - START) / 1000.0 >= timeout_in_secs * (99.0 / 100.0)) {
                throw new TimeoutException();
            }
        }

        public String playerName() {
            return "Schillaci_L0";
        }
    }
