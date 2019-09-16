package flip.g6;

import java.util.HashMap;
import java.util.Random;

import flip.sim.Board;
import flip.sim.Point;
import javafx.util.Pair;

public class ObstacleCreation extends Move {
	
	private HashMap<Integer, Point> player_pieces;
	private HashMap<Integer, Point> opponent_pieces;
	private boolean isplayer1;
	private Integer n;
	private Double diameter_piece;
	
	public ObstacleCreation(HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, boolean isplayer1, Integer n, Double diameter_piece) {
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

		HashMap<Integer, Point> unfinished_pieces = getUnfinishedPlayerPieces(player_pieces, isplayer1, Approach.CREATION);
		HashMap<Integer, Point> closest_pieces = getClosestPointsToOpponentBoundary(n / 2, player_pieces, isplayer1);
		HashMap<Integer, Point> relevant_pieces = (HashMap<Integer, Point>) player_pieces.clone();
		HashMap<Integer, Point> relevant_unfinished_pieces = new HashMap<>();		
		
		for(Integer id : closest_pieces.keySet()) {
			relevant_pieces.remove(id);
		}
		
		for(Integer id : relevant_pieces.keySet())
			if(unfinished_pieces.containsKey(id) && relevant_pieces.containsKey(id))
				relevant_unfinished_pieces.put(id, relevant_pieces.get(id));		
		
		//System.out.println("Number of OC unfinished pieces: " + unfinished_pieces.size());

		Random random = new Random();
		Integer piece_id = random.nextInt(n);
		while(!relevant_unfinished_pieces.containsKey(piece_id))
			piece_id = random.nextInt(n);
		Point curr_position = player_pieces.get(piece_id);
		Point new_position = new Point(curr_position);

//		double maxInteriorDistance = 0;
//		if(isplayer1) {
//			maxInteriorDistance = 20;
//		}else {
//			maxInteriorDistance = -20;
//		}

//		if((isplayer1 && curr_position.x < maxInteriorDistance) || (!isplayer1 && curr_position.x > maxInteriorDistance)) {
			// Player 1
//			try {
//				return getMove();
//				return null;
//			} catch (Exception e) {
				// TODO Auto-generated catch block
//				return null;
//			}
//		}

		double theta = -Math.PI/2 + Math.PI * random.nextDouble();
		double delta_x = 0;
		double delta_y = 0;
		delta_x = diameter_piece * Math.cos(theta);
		delta_y = diameter_piece * Math.sin(theta);

		Double val = (Math.pow(delta_x,2) + Math.pow(delta_y, 2));
		// System.out.println("delta_x^2 + delta_y^2 = " + val.toString() + " theta values are " + Math.cos(theta) + " " + Math.sin(theta) + " diameter is " + diameter_piece);
		// Log.record("delta_x^2 + delta_y^2 = " + val.toString() + " theta values are " + Math.cos(theta) + " " + Math.sin(theta) + " diameter is " + diameter_piece);

		new_position.x = isplayer1 ? new_position.x - delta_x : new_position.x + delta_x;
		new_position.y += delta_y;
		move = new Pair<Integer, Point>(piece_id, new_position);

		Double dist = Board.getdist(player_pieces.get(move.getKey()), move.getValue());
		// System.out.println("distance from previous position is " + dist.toString());
		// Log.record("distance from previous position is " + dist.toString());
		
		return move;
	}
	
	@Override
	public Pair<Integer, Point> getHybridMove() {
		Pair<Integer, Point> move = null; // TODO: Change this implementation
		return move;
	}

}
