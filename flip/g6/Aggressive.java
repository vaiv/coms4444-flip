package flip.g6;

import java.util.HashMap;
import java.util.Random;

import flip.sim.Board;
import flip.sim.Point;
import javafx.util.Pair;

public class Aggressive extends Move {

	private HashMap<Integer, Point> player_pieces;
	private HashMap<Integer, Point> opponent_pieces;
	private boolean isplayer1;
	private Integer n;
	private Double diameter_piece;
	
	public Aggressive(HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, boolean isplayer1, Integer n, Double diameter_piece) {
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

		HashMap<Integer, Point> unfinished_pieces = getUnfinishedPlayerPieces(player_pieces, isplayer1);
		HashMap<Integer, Point> closest_pieces = getClosestPointsToOpponentBoundary(n / 2, unfinished_pieces, isplayer1);

		Random random = new Random();
		Integer piece_id = random.nextInt(n);

		while(!closest_pieces.containsKey(piece_id))
			piece_id = random.nextInt(n);

		Point curr_position = player_pieces.get(piece_id);
	 	Point new_position = new Point(curr_position);
		
		double theta = 0;
	 	double delta_x = diameter_piece * Math.cos(theta);
	 	double delta_y = diameter_piece * Math.sin(theta);

	 	Double val = (Math.pow(delta_x,2) + Math.pow(delta_y, 2));
	 	// System.out.println("delta_x^2 + delta_y^2 = " + val.toString() + " theta values are " +  Math.cos(theta) + " " +  Math.sin(theta) + " diameter is " + diameter_piece);
	 	// Log.record("delta_x^2 + delta_y^2 = " + val.toString() + " theta values are " +  Math.cos(theta) + " " +  Math.sin(theta) + " diameter is " + diameter_piece);

	 	new_position.x = isplayer1 ? new_position.x - delta_x : new_position.x + delta_x;
	 	new_position.y += delta_y;
	 	move = new Pair<Integer, Point>(piece_id, new_position);

	 	Double dist = Board.getdist(player_pieces.get(move.getKey()), move.getValue());

	 	if(checkValidity(move, player_pieces, opponent_pieces, diameter_piece))
	 		return move;
	 	
		return new ObstacleCreation(player_pieces, opponent_pieces, isplayer1, n, diameter_piece).getMove();

	}
	
	@Override
	public Pair<Integer, Point> getHybridMove() {
		Pair<Integer, Point> move = null; // TODO: Change this implementation
		return move;
	}
}
