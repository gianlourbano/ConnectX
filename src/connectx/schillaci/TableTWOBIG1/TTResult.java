package connectx.schillaci.TableTWOBIG1;

public class TTResult {
    public static enum type {
        EXACT, LOWER_BOUND, UPPER_BOUND
    }

    public int bestMove;
    public int score;
    public type flag;
    public int depth;
    public long searchedNodes;

    public TTResult(int bestMove, int score, type flag, int depth, long searchedNodes) {
        this.bestMove = bestMove;
        this.score = score;
        this.flag = flag;
        this.depth = depth;
        this.searchedNodes = searchedNodes;
    }

    public int compare(TTResult r) {
        if(this.searchedNodes < r.searchedNodes) 
            return -1;
        if(this.searchedNodes == r.searchedNodes) 
            return 0;
        return 1;
    }
}
