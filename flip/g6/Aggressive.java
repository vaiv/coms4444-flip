package flip.g6;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
	public void updatePieceInfo(HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces) {
		this.player_pieces = player_pieces;
		this.opponent_pieces = opponent_pieces;
	}
	
	@Override
	public boolean isPossible() {
		return false; // TODO: Change this implementation
	}

	@Override
	public Pair<Integer, Point> getMove() {

		Pair<Integer, Point> move = null; // TODO: Change this implementation		
		switch(n) {
		case 1: {
			for(Integer piece_id : player_pieces.keySet()) {
				Point curr_position = player_pieces.get(piece_id);
				Point new_position;

				double theta = 0;
				double numAngles = 180;
				for(int i = 0; i <= numAngles; i++) {
					if(i % 2 == 0)
						theta -= i * Math.PI / numAngles;
					else
						theta += i * Math.PI / numAngles;
					double delta_x = diameter_piece * Math.cos(theta);
					double delta_y = diameter_piece * Math.sin(theta);

					Double val = (Math.pow(delta_x,2) + Math.pow(delta_y, 2));
					// System.out.println("delta_x^2 + delta_y^2 = " + val.toString() + " theta values are " +  Math.cos(theta) + " " +  Math.sin(theta) + " diameter is " + diameter_piece);
					// Log.record("delta_x^2 + delta_y^2 = " + val.toString() + " theta values are " +  Math.cos(theta) + " " +  Math.sin(theta) + " diameter is " + diameter_piece);

					new_position = isplayer1 ? new Point(curr_position.x - delta_x, curr_position.y + delta_y) : new Point(curr_position.x + delta_x, curr_position.y + delta_y);
					move = new Pair<Integer, Point>(piece_id, new_position);

					Double dist = Board.getdist(player_pieces.get(move.getKey()), move.getValue());

					//System.out.println("         Aggressive, theta: " + theta);

					if(checkValidity(move, player_pieces, opponent_pieces, diameter_piece)) {
						//System.out.println("THETA USED...Aggressive, theta: " + theta);
						System.out.println("n = 1, aggressive, new position: " + new_position);
						player_pieces.put(piece_id, new_position);
						return move;
					}
				}
			}
			return null;
		}
		case 2:
		case 3:
		case 4:
		case 5:
		case 6:
		case 7:
		case 8:
		case 9:
		case 10: 
		case 11: {
			HashMap<Integer, Point> unfinished_pieces = getUnfinishedPlayerPieces(player_pieces, isplayer1, Approach.AGGRESSIVE);
			HashMap<Integer, Point> closest_pieces = getClosestPointsToOpponentBoundary(unfinished_pieces.size(), unfinished_pieces, isplayer1);
			double posMarker = isplayer1 ? 10.0 : -10.0;
			List<Integer> ids = new ArrayList<>();
			for(Integer piece_id : closest_pieces.keySet()) {
				if((isplayer1 && (closest_pieces.get(piece_id).x >= posMarker)) || (!isplayer1 && (closest_pieces.get(piece_id).x <= posMarker))) {
					Point curr_position = player_pieces.get(piece_id);
					Point new_position;
					double theta = 0;
					double delta_x = diameter_piece * Math.cos(theta);
					double delta_y = diameter_piece * Math.sin(theta);

					//				 	Double val = (Math.pow(delta_x,2) + Math.pow(delta_y, 2));
					// System.out.println("delta_x^2 + delta_y^2 = " + val.toString() + " theta values are " +  Math.cos(theta) + " " +  Math.sin(theta) + " diameter is " + diameter_piece);
					// Log.record("delta_x^2 + delta_y^2 = " + val.toString() + " theta values are " +  Math.cos(theta) + " " +  Math.sin(theta) + " diameter is " + diameter_piece);

					new_position = isplayer1 ? new Point(curr_position.x - delta_x, curr_position.y + delta_y) : new Point(curr_position.x + delta_x, curr_position.y + delta_y);
					move = new Pair<Integer, Point>(piece_id, new_position);

					Double dist = Board.getdist(player_pieces.get(move.getKey()), move.getValue());

					//System.out.println("         Aggressive, theta: " + theta);

					if(checkValidity(move, player_pieces, opponent_pieces, diameter_piece)) {
						//System.out.println("THETA USED...Aggressive, theta: " + theta);
						player_pieces.put(piece_id, new_position);
						return move;
					}
				}
			}
			for(Integer piece_id : closest_pieces.keySet()) {
				if((isplayer1 && (closest_pieces.get(piece_id).x < posMarker)) || (!isplayer1 && (closest_pieces.get(piece_id).x > posMarker))) {
					Point curr_position = player_pieces.get(piece_id);
					Point new_position;
					double theta = 0;
					double delta_x = diameter_piece * Math.cos(theta);
					double delta_y = diameter_piece * Math.sin(theta);

					//				 	Double val = (Math.pow(delta_x,2) + Math.pow(delta_y, 2));
					// System.out.println("delta_x^2 + delta_y^2 = " + val.toString() + " theta values are " +  Math.cos(theta) + " " +  Math.sin(theta) + " diameter is " + diameter_piece);
					// Log.record("delta_x^2 + delta_y^2 = " + val.toString() + " theta values are " +  Math.cos(theta) + " " +  Math.sin(theta) + " diameter is " + diameter_piece);

					new_position = isplayer1 ? new Point(curr_position.x - delta_x, curr_position.y + delta_y) : new Point(curr_position.x + delta_x, curr_position.y + delta_y);
					move = new Pair<Integer, Point>(piece_id, new_position);

					Double dist = Board.getdist(player_pieces.get(move.getKey()), move.getValue());

					//System.out.println("         Aggressive, theta: " + theta);

					if(checkValidity(move, player_pieces, opponent_pieces, diameter_piece)) {
						//System.out.println("THETA USED...Aggressive, theta: " + theta);
						player_pieces.put(piece_id, new_position);
						return move;
					}
				}
			}
			for(Integer piece_id : closest_pieces.keySet()) {
				Point curr_position = player_pieces.get(piece_id);
				Point new_position;

				double theta = 0;
				double numAngles = 180;
				for(int i = 0; i <= numAngles; i++) {
					if(i % 2 == 0)
						theta -= i * Math.PI / numAngles;
					else
						theta += i * Math.PI / numAngles;
					double delta_x = diameter_piece * Math.cos(theta);
					double delta_y = diameter_piece * Math.sin(theta);

					Double val = (Math.pow(delta_x,2) + Math.pow(delta_y, 2));
					// System.out.println("delta_x^2 + delta_y^2 = " + val.toString() + " theta values are " +  Math.cos(theta) + " " +  Math.sin(theta) + " diameter is " + diameter_piece);
					// Log.record("delta_x^2 + delta_y^2 = " + val.toString() + " theta values are " +  Math.cos(theta) + " " +  Math.sin(theta) + " diameter is " + diameter_piece);

					new_position = isplayer1 ? new Point(curr_position.x - delta_x, curr_position.y + delta_y) : new Point(curr_position.x + delta_x, curr_position.y + delta_y);
					move = new Pair<Integer, Point>(piece_id, new_position);

					Double dist = Board.getdist(player_pieces.get(move.getKey()), move.getValue());

					//System.out.println("         Aggressive, theta: " + theta);

					if(checkValidity(move, player_pieces, opponent_pieces, diameter_piece)) {
						//System.out.println("THETA USED...Aggressive, theta: " + theta);
						player_pieces.put(piece_id, new_position);
						return move;
					}
				}
			}
			return null;
		}
		default: {
			HashMap<Integer, Point> unfinished_pieces = getUnfinishedPlayerPieces(player_pieces, isplayer1, Approach.AGGRESSIVE);
			HashMap<Integer, Point> closest_pieces = getClosestPointsToOpponentBoundary((int) Math.ceil(unfinished_pieces.size() / 2.0), unfinished_pieces, isplayer1);
			HashMap<Integer, Point> closest_unfinished_pieces = new HashMap<>();

			for(Integer id : closest_pieces.keySet())
				if(unfinished_pieces.containsKey(id) && closest_pieces.containsKey(id))
					closest_unfinished_pieces.put(id, closest_pieces.get(id));

			//			System.out.println("Number of A unfinished pieces: " + unfinished_pieces.size());

			List<Integer> ids = new ArrayList<>();
			for(Integer id : closest_unfinished_pieces.keySet()) {
				ids.add(id);
			}

			while(ids.size() > 0) {

				Random random = new Random();
				Integer piece_id = ids.get(random.nextInt(ids.size()));


				piece_id = random.nextInt(n);

				Point curr_position = player_pieces.get(piece_id);
				Point new_position;

				double theta = 0;
				double numAngles = 180;
				for(int i = 0; i <= numAngles; i++) {
					if(i % 2 == 0)
						theta -= i * Math.PI / numAngles;
					else
						theta += i * Math.PI / numAngles;
					double delta_x = diameter_piece * Math.cos(theta);
					double delta_y = diameter_piece * Math.sin(theta);

					Double val = (Math.pow(delta_x,2) + Math.pow(delta_y, 2));
					// System.out.println("delta_x^2 + delta_y^2 = " + val.toString() + " theta values are " +  Math.cos(theta) + " " +  Math.sin(theta) + " diameter is " + diameter_piece);
					// Log.record("delta_x^2 + delta_y^2 = " + val.toString() + " theta values are " +  Math.cos(theta) + " " +  Math.sin(theta) + " diameter is " + diameter_piece);

					new_position = isplayer1 ? new Point(curr_position.x - delta_x, curr_position.y + delta_y) : new Point(curr_position.x + delta_x, curr_position.y + delta_y);
					move = new Pair<Integer, Point>(piece_id, new_position);

					Double dist = Board.getdist(player_pieces.get(move.getKey()), move.getValue());

					//System.out.println("         Aggressive, theta: " + theta);

					if(checkValidity(move, player_pieces, opponent_pieces, diameter_piece)) {
						//System.out.println("THETA USED...Aggressive, theta: " + theta);
						player_pieces.put(piece_id, new_position);
						return move;
					}
				}
			}		
			return null;
		}
		}		
	}

	@Override
	public Pair<Integer, Point> getHybridMove() {
		Pair<Integer, Point> move = null; // TODO: Change this implementation
		return move;
	}
}