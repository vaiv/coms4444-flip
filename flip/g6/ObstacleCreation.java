package flip.g6;

import java.util.HashMap;

import flip.sim.Point;
import javafx.util.Pair;

public class ObstacleCreation implements Move {

	private HashMap<Integer, Point> player_pieces;
	private HashMap<Integer, Point> opponent_pieces;
	private boolean isplayer1;
	
	public ObstacleCreation(HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, boolean isplayer1) {
		this.player_pieces = player_pieces;
		this.opponent_pieces = opponent_pieces;
		this.isplayer1 = isplayer1;
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
