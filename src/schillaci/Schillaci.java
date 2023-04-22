package schillaci;

import connectx.CXBoard;
import connectx.CXPlayer;

public class Schillaci implements CXPlayer {
    private int M, N, X;
    private boolean first;
    private int timeout_in_secs;
    
    public void initPlayer(int M, int N, int X, boolean first, int timeout_in_secs) {
        this.M = M;
        this.N = N;
        this.X = X;
        this.first = first;
        this.timeout_in_secs = timeout_in_secs;
    }

    public int selectColumn(CXBoard B) {
        return 0;
    }

    public String playerName() {
        return "Schillaci";
    }
}
