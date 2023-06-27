package schillaci;

import java.util.ArrayList;

import connectx.CXBoard;
import connectx.CXCellState;

public class Evaluator {
	private int M, N, K;

	private int player1Position = CXCellState.P1.ordinal();
	private int player2Position = CXCellState.P2.ordinal();
	private int emptyPosition = CXCellState.FREE.ordinal();

	private int[][] positionWeights;
	private int[][] winSequence;
	private int[][] openEndSequence;
	private int[][] threatSequence;
	private int[][] sevenTrapSequence;

	Evaluator(int M, int N, int K) {
		this.M = M;
		this.N = N;
		this.K = K;

		createPositionWeights();
		createWinSequence();
		createOpenEndSequence();
		createThreatSequence();
		createSevenTrapSequence();
	}

	private boolean isInBounds(int row, int col) {
		return row >= 0 && row < M && col >= 0 && col < N;
	}

	private boolean match(CXBoard B, int x, int y, int[] seq, int deltaX, int deltaY, int deltaS) {
		int s = deltaS > 0 ? 0 : seq.length - 1;

		if (isInBounds(x + deltaX * (seq.length - 1), y + deltaY * (seq.length - 1))) {
			for (int i = 0; i < seq.length; i++) {
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

	private boolean matchPosition(CXBoard B, int x, int y, int[] seq, int dir) {
		if (match(B, x, y, seq, 1, 0, dir))
			return true;
		if (match(B, x, y, seq, 0, 1, dir))
			return true;
		if (match(B, x, y, seq, 1, 1, dir))
			return true;
		if (match(B, x, y, seq, 1, -1, dir))
			return true;

		return false;
	}

	private int countPositionForward(CXBoard B, int[] seq) {
		int seq_count = 0;

		for (int i = 0; i < M; i++) {
			for (int j = 0; j < N; j++) {
				if (matchPosition(B, i, j, seq, 1)) {
					seq_count++;
				}
			}
		}

		return seq_count;
	}

	private int countPositionBackward(CXBoard B, int[] seq) {
		int seq_count = 0;

		for (int i = M - 1; i >= 0; i--) {
			for (int j = N - 1; j >= 0; j--) {
				if (matchPosition(B, i, j, seq, -1)) {
					seq_count++;
				}
			}
		}

		return seq_count;
	}

	private int evalWins(CXBoard B, int player) {

		return countPositionForward(B, winSequence[player]);
	}

	private void createWinSequence() {
		winSequence = new int[3][K];

		for (int j = 0; j < K; j++) {
			winSequence[player1Position][j] = player1Position;
			winSequence[player2Position][j] = player2Position;
		}
	}

	private int evalThreats(CXBoard B, int player) {

		return (countPositionBackward(B, threatSequence[player]) + countPositionForward(B, threatSequence[player]));
	}

	private void createThreatSequence() {
		threatSequence = new int[3][K];

		threatSequence[player1Position][0] = emptyPosition;
		threatSequence[player2Position][0] = emptyPosition;
		for (int j = 0; j < K; j++) {
			threatSequence[player1Position][j] = player1Position;
			threatSequence[player2Position][j] = player2Position;
		}
	}

	private int evalOpenEnds(CXBoard B, int player) {

		return (countPositionBackward(B, openEndSequence[player]) + countPositionForward(B, openEndSequence[player]));
	}

	private void createOpenEndSequence() {
		openEndSequence = new int[3][K + 1];

		openEndSequence[player1Position][0] = emptyPosition;
		openEndSequence[player2Position][0] = emptyPosition;

		for (int j = 0; j < K; j++) {
			openEndSequence[player1Position][j + 1] = player1Position;
			openEndSequence[player2Position][j + 1] = player2Position;
		}

		openEndSequence[player1Position][K] = emptyPosition;
		openEndSequence[player2Position][K] = emptyPosition;
	}

	private int evalSevenTraps(CXBoard B, int player) {

		int sevenTraps = 0;

		for (int i = 0; i < M; i++) {
			for (int j = 0; j < N - 1; j++) {
				if (match(B, i, j, sevenTrapSequence[player], -1, 0, 1)) {
					if (match(B, i, j + 1, sevenTrapSequence[player], -1, -1, 1)) {
						sevenTraps++;
					}
				}

				if (match(B, i, j, sevenTrapSequence[player], 1, 0, 1)) {
					if (match(B, i, j + 1, sevenTrapSequence[player], 1, -1, 1)) {
						sevenTraps++;
					}
				}
			}
		}

		for (int i = 0; i < M; i++) {
			for (int j = 1; j < N; j++) {
				if (match(B, i, j, sevenTrapSequence[player], -1, 0, 1)) {
					if (match(B, i, j - 1, sevenTrapSequence[player], -1, 1, 1)) {
						sevenTraps++;
					}
				}

				if (match(B, i, j, sevenTrapSequence[player], 1, 0, 1)) {
					if (match(B, i, j - 1, sevenTrapSequence[player], 1, 1, 1)) {
						sevenTraps++;
					}
				}
			}
		}

		return sevenTraps;
	}

	private void createSevenTrapSequence() {
		sevenTrapSequence = new int[3][K + 1];

		sevenTrapSequence[player1Position][0] = emptyPosition;
		sevenTrapSequence[player2Position][0] = emptyPosition;

		for (int j = 0; j < K; j++) {
			sevenTrapSequence[player1Position][j + 1] = player1Position;
			sevenTrapSequence[player2Position][j + 1] = player2Position;
		}
	}

	private int evalPositionWeights(CXBoard B, int player) {

		int score = 0;

		for (int i = 0; i < M; i++) {
			for (int j = 0; j < N; j++) {
				if (B.cellState(i, j).ordinal() == player) {
					score += positionWeights[i][j];
				}
			}
		}

		return score;

	}

	private void createPositionWeights() {
		positionWeights = new int[M][N];
		int k = K;

		for (int i = 0; i < M; i++) {
			for (int j = 0; j < N; j++) {
				if (isInBounds(i + (k - 1), j)) {
					for (int l = 0; l < k; l++) {
						positionWeights[i + l][j]++;
					}
				}

				if (isInBounds(i, j + (k - 1))) {
					for (int l = 0; l < k; l++) {
						positionWeights[i][j + l]++;
					}
				}

				if (isInBounds(i + (k - 1), j + (k - 1))) {
					for (int l = 0; l < k; l++) {
						positionWeights[i + l][j + l]++;
					}
				}

				if (isInBounds(i + (k - 1), j - (k - 1))) {
					for (int l = 0; l < k; l++) {
						positionWeights[i + l][j - l]++;
					}
				}
			}
		}
	}

	private final int[] evalWeights = { 1000000, 1000, 100, 150, 400 };

	//
	// eval() is a MFEF that returns the score for a given game state by combining 5
	// weighted factors
	//
	public int eval(CXBoard state) {
		int[] aiScores    = {0, 0, 0, 0, 0};
		int[] humanScores = {0, 0, 0, 0, 0};
		
		aiScores[0]    = evalWins(state, 0);
		humanScores[0] = evalWins(state, 1);
		
		aiScores[1]    = evalThreats(state, 0);
		humanScores[1] = evalThreats(state, 1);
		
		aiScores[2]    = evalOpenEnds(state, 0);
		humanScores[2] = evalOpenEnds(state, 1);
		
		aiScores[3]    = evalPositionWeights(state, 0);
		humanScores[3] = evalPositionWeights(state, 1);
		
		aiScores[4]    = evalSevenTraps(state, 0);
		humanScores[4] = evalSevenTraps(state, 1);
		
		int finalScore = 0;
		
		for (int i = 0; i < aiScores.length; i++) {
			finalScore += (evalWeights[i] * (aiScores[i] - humanScores[i]));
		}
		
		return finalScore;
	}

}
