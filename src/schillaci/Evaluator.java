package schillaci;

import java.util.ArrayList;

import connectx.CXBoard;
import connectx.CXCellState;

public class Evaluator {
	private static int M, N, K;

	private static int emptyPosition = CXCellState.FREE.ordinal();
	private static int player1Position = CXCellState.P1.ordinal();
	private static int player2Position = CXCellState.P2.ordinal();

	private static int[][] positionWeights;
	private static int[][] winSequence;
	private static int[][] openEndSequence;
	private static int[][] threatSequence;
	private static int[][] sevenTrapSequence;

	Evaluator(int M, int N, int K) {
		this.M = M;
		this.N = N;
		this.K = K;
	}

	private static boolean isInBounds(int row, int col) {
		return row >= 0 && row < M && col >= 0 && col < N;
	}

	private static boolean match(CXBoard B, int x, int y, int[] seq, int deltaX, int deltaY, int deltaS) {
		int s = deltaS > 0 ? 0 : seq.length - 1;

		if (isInBounds(x + deltaX * (seq.length - 1), y + deltaY * (seq.length - 1))) {
			for (int i = 0; i< seq.length; i++) {
				if (B.cellState(x, y).ordinal() != seq[s])
				return false;

				x += deltaX;
				y += deltaY;
				s += deltaS;
			}

			return true;
		}
		return false;
	}

	private static boolean matchPosition(CXBoard B, int x, int y, int[] seq, int dir) {
		if(match(B, x, y, seq, 1, 0, dir)) return true;
		if(match(B, x, y, seq, 0, 1, dir)) return true;
		if(match(B, x, y, seq, 1, 1, dir)) return true;
		if(match(B, x, y, seq, 1, -1, dir)) return true;

		return false;
	}

	private static int countPositionForward(CXBoard B, int[] seq) {
		int seq_count = 0;

		for(int i = 0; i < M; i++) {
			for(int j = 0; j < N; j++) {
				if(matchPosition(B, i, j, seq, 1)) {
					seq_count++;
				}
			}
		}

		return seq_count;
	}

	private static int countPositionBackward(CXBoard B, int[] seq) {
		int seq_count = 0;

		for(int i = M - 1; i >= 0; i--) {
			for(int j = N - 1; j >= 0; j--) {
				if(matchPosition(B, i, j, seq, -1)) {
					seq_count++;
				}
			}
		}

		return seq_count;
	}

	private static int evalWins(CXBoard B) {
		int player = B.currentPlayer();
		
		return countPositionForward(B, winSequence[player]);
	}
	
	
}
