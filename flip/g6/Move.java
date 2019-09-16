package flip.g6;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
		HashMap<Integer, Point> closest_pieces = new HashMap<>();
		HashMap<Integer, Point> sorted_pieces = new HashMap<>();
		for(Integer key : player_pieces.keySet())
			sorted_pieces.put(key, player_pieces.get(key));
		Object[] sorted_pieces_array = sorted_pieces.entrySet().toArray();
		if(isPlayer1) {
			Arrays.sort(sorted_pieces_array, new Comparator() {
			    public int compare(Object o1, Object o2) {
			        return ((Double) (((Map.Entry<Integer, Point>) o1).getValue().x)).compareTo((Double) (((Map.Entry<Integer, Point>) o2).getValue().x));
			    }
			});			
		}
		else {
			Arrays.sort(sorted_pieces_array, new Comparator() {
			    public int compare(Object o1, Object o2) {
			        return ((Double) (((Map.Entry<Integer, Point>) o2).getValue().x)).compareTo((Double) (((Map.Entry<Integer, Point>) o1).getValue().x));
			    }
			});
			
		}
		List<Integer> ids = new ArrayList<>();
		for (Object sorted_piece : sorted_pieces_array)
			ids.add(((Map.Entry<Integer, Point>) sorted_piece).getKey());
		for(int i = 0; i < numPoints; i++)
			closest_pieces.put(ids.get(i), sorted_pieces.get(ids.get(i)));
		
		return closest_pieces;
	}
	
	public Pair<Integer, Point> getClosestOpponent(Pair<Integer, Point> player_piece, HashMap<Integer, Point> opponent_pieces, boolean isPlayer1) {
		return null;
	}
	
	public HashMap<Integer, Point> getUnfinishedPlayerPieces(HashMap<Integer, Point> player_pieces, boolean isplayer1) {
		HashMap<Integer, Point> unfinished_player_pieces = new HashMap<>();
		double maxInteriorDistance = player_pieces.size() / 10 + 3;
		for(Integer i : player_pieces.keySet()) {			
			Point curr_position = player_pieces.get(i);
		 	if((isplayer1 && curr_position.x < -(20 + maxInteriorDistance)) || (!isplayer1 && curr_position.x > (20 + maxInteriorDistance)))
		 		continue;
		 	unfinished_player_pieces.put(i, curr_position);
		}
		return unfinished_player_pieces;
	}
}
