package flip.g6;

import java.util.HashMap;

import flip.sim.Point;
import javafx.util.Pair;

public class ObstacleAvoidance extends Move {
	
	private HashMap<Integer, Point> player_pieces;
	private HashMap<Integer, Point> opponent_pieces;
	private boolean isplayer1;
	private Integer n;
	private Double diameter_piece;
	
	public ObstacleAvoidance(HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, boolean isplayer1, Integer n, Double diameter_piece) {
		this.player_pieces = player_pieces;
		this.opponent_pieces = opponent_pieces;
		this.isplayer1 = isplayer1;
		this.n = n;
		this.diameter_piece = diameter_piece;
	}

	@Override
	public boolean isPossible() {
		return true; // TODO: Change this implementation
	}
	
	@Override
	public Pair<Integer, Point> getMove() {
		Pair<Integer, Point> move = null; // TODO: Change this implementation
		return move;
	}
	
	@Override
	public Pair<Integer, Point> getHybridMove() {
		Pair<Integer, Point> move = null; // TODO: Change this implementation
		return move;
	}

}
