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
	
	public abstract Pair<Integer, Point> getMove(); // Returns normal move for the specific strategy
	
	enum Approach {
		AGGRESSIVE, AVOIDANCE, CREATION;
	}	
	
	/**
	 * Checks the validity of a coin's  move
	 * @param move: New move made by the coin
	 * @param playerPieces: All player pieces
	 * @param opponentPieces: All opponent pieces
	 * @param diameterPiece: Diameter of each piece
	 * @return boolean to test move validity
	 */
	public boolean checkValidity(Pair<Integer, Point> move, HashMap<Integer, Point> playerPieces, HashMap<Integer, Point> opponentPieces, Double diameterPiece) {
		boolean valid = true;

		// check if move is adjacent to previous position.
		if(!Board.almostEqual(Board.getdist(playerPieces.get(move.getKey()), move.getValue()), diameterPiece))
		{
			return false;
		}
		// check for collisions
		valid = valid && !Board.check_collision(playerPieces, move);
		valid = valid && !Board.check_collision(opponentPieces, move);

		// check within bounds
		valid = valid && Board.check_within_bounds(move);
		return valid;

	}
	
	/**
	 * Get n closest points to opponent boundary
	 * @param numPoints: Required closest points
	 * @param playerPieces: Player pieces
	 * @param isPlayer1: Flag to check if player is player one or not
	 * @return n closest points
	 */
	public HashMap<Integer, Point> getClosestPointsToOpponentBoundary(int numPoints, HashMap<Integer, Point> playerPieces, boolean isPlayer1) {
		HashMap<Integer, Point> closest_pieces = new HashMap<>();
		HashMap<Integer, Point> sorted_pieces = new HashMap<>();
		for(Integer key : playerPieces.keySet())
			sorted_pieces.put(key, playerPieces.get(key));
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

	/**
	 * Get n closest points to opponent boundary
	 * @param playerPieces: Player pieces
	 * @param isplayer1: Flag to check if player is player one or not
	 * @param approach: Approach taken by the player
	 * @return
	 */
	public HashMap<Integer, Point> getUnfinishedPlayerPieces(HashMap<Integer, Point> playerPieces, boolean isplayer1, Approach approach) {
		HashMap<Integer, Point> unfinished_player_pieces = new HashMap<>();
		
		switch(approach) {
			case AGGRESSIVE: {
				double maxInteriorDistance = playerPieces.size() / 10 + 1.5;
				for(Integer i : playerPieces.keySet()) {			
					Point curr_position = playerPieces.get(i);
				 	if((isplayer1 && curr_position.x < -(20 + maxInteriorDistance)) || (!isplayer1 && curr_position.x > (20 + maxInteriorDistance)))
				 		continue;
				 	unfinished_player_pieces.put(i, curr_position);
				}
				return unfinished_player_pieces;
			}
			case AVOIDANCE: {
				return null;
			}
			case CREATION: {
				double maxInteriorDistance = 0;
				if(isplayer1) {
					maxInteriorDistance = 21;
				}
				else {
					maxInteriorDistance = -21;
				}

				for(Integer i : playerPieces.keySet()) {
					Point curr_position = playerPieces.get(i);
					if((isplayer1 && curr_position.x < maxInteriorDistance) || (!isplayer1 && curr_position.x > maxInteriorDistance))
						continue;
					unfinished_player_pieces.put(i, curr_position);
				}
				return unfinished_player_pieces;				
			}
			default: return null;
		}
	}
	
	/**
	 * Updates the pieces
	 * @param playerPieces: Player pieces
	 * @param opponentPieces: All opponent pieces
	 */
	public abstract void updatePieceInfo(HashMap<Integer, Point> playerPieces, HashMap<Integer, Point> opponentPieces);

}