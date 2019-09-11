package flip.g6;

import java.util.HashMap;

import flip.sim.Board;
import flip.sim.Point;
import javafx.util.Pair;

public abstract class Move {
	
	public abstract boolean isPossible(); // Returns if any of the coins selected for a strategy can perform that strategy
	public abstract Pair<Integer, Point> getMove(); // Returns normal move for the specific strategy
	public abstract Pair<Integer, Point> getHybridMove(); // Returns move for a different strategy
	
	public boolean checkValidity(Pair<Integer, Point> move, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, Double diameter_piece) {
		boolean valid = true;

		// check if move is adjacent to previous position.
		if(!Board.almostEqual(Board.getdist(player_pieces.get(move.getKey()), move.getValue()), diameter_piece))
		{
			return false;
		}
		// check for collisions
		valid = valid && !Board.check_collision(player_pieces, move);
		valid = valid && !Board.check_collision(opponent_pieces, move);

		// check within bounds
		valid = valid && Board.check_within_bounds(move);
		return valid;

	}
	
	public HashMap<Integer, Point> getClosestPointsToOpponentBoundary(int numPoints, HashMap<Integer, Point> player_pieces, boolean isPlayer1) {
		return null;
	}
	
	public Pair<Integer, Point> getClosestOpponent(Pair<Integer, Point> player_piece, HashMap<Integer, Point> opponent_pieces, boolean isPlayer1) {
		return null;
	}
}
