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
	public boolean RUNNER_STRATEGY_SET = true;
	private int highPieceID = -1;
	private int lowPieceID = -1;
	int counter = 0;

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

		Pair<Integer, Point> move = null;
		if(n == 1) {			
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
		else if(n <= 15) {
			HashMap<Integer, Point> unfinished_pieces = getUnfinishedPlayerPieces(player_pieces, isplayer1, Approach.AGGRESSIVE);
			HashMap<Integer, Point> closest_pieces = getClosestPointsToOpponentBoundary(unfinished_pieces.size(), unfinished_pieces, isplayer1);
			double posMarker = isplayer1 ? 15.0 : -15.0;
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
			return null;		}
		else {
			double startingBoundaryForAggressive = isplayer1 ? 19.5 : -19.5;
			HashMap<Integer, Point> unfinished_pieces = getUnfinishedPlayerPieces(player_pieces, isplayer1, Approach.AGGRESSIVE);
			HashMap<Integer, Point> closest_pieces = getClosestPointsToOpponentBoundary(unfinished_pieces.size(), unfinished_pieces, isplayer1);
			for(Integer pieceID : closest_pieces.keySet()) {
				if((isplayer1 && closest_pieces.get(pieceID).x < startingBoundaryForAggressive) || (!isplayer1 && closest_pieces.get(pieceID).x > startingBoundaryForAggressive)) {
					Point curr_position = player_pieces.get(pieceID);
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

						new_position = isplayer1 ? new Point(curr_position.x - delta_x, curr_position.y + delta_y) : new Point(curr_position.x + delta_x, curr_position.y + delta_y);
						move = new Pair<Integer, Point>(pieceID, new_position);

						if(checkValidity(move, player_pieces, opponent_pieces, diameter_piece)) {
							//System.out.println("THETA USED...Aggressive, theta: " + theta);
							player_pieces.put(pieceID, new_position);
							return move;
						}
					}
				}
			}
		}
		return null;
	}

	public Pair<Integer, Point> run(Integer numMoves, boolean isEvenMove) {
		// Top coin move
		if(highPieceID == -1) {
			Point highestPoint = isplayer1 ? new Point(20.0, 20.0) : new Point(-20.0, 20.0);
			highPieceID = 0;
			double distance = Double.MAX_VALUE;
			for(Integer index : player_pieces.keySet()) {
				double newDistance = Math.sqrt(Math.pow(highestPoint.x - player_pieces.get(index).x, 2) + Math.pow(highestPoint.y - player_pieces.get(index).y, 2));
				if(newDistance < distance) {
					distance = newDistance;
					highPieceID = index;
				}
			}
		}
		
		// Lowest coin move
		if(lowPieceID == -1) {
			Point lowestPoint = isplayer1 ? new Point(20.0, -20.0) : new Point(-20.0, -20.0);
			lowPieceID = 0;
			double distance = Double.MAX_VALUE;
			for(Integer index : player_pieces.keySet()) {
				double newDistance = Math.sqrt(Math.pow(lowestPoint.x - player_pieces.get(index).x, 2) + Math.pow(lowestPoint.y - player_pieces.get(index).y, 2));
				if(newDistance < distance) {
					distance = newDistance;
					lowPieceID = index;
				}
			}
			
		}
		
		if(isEvenMove && counter < 5) {
			Point curr_position = player_pieces.get(highPieceID);
			Point new_position = new Point(curr_position);
			double theta = 0;
			double delta_x = diameter_piece * Math.cos(theta);
			double delta_y = diameter_piece * Math.sin(theta);
			new_position.x = isplayer1 ? new_position.x - delta_x: new_position.x + delta_x;
			new_position.y += delta_y;
			player_pieces.put(highPieceID, new_position);
			counter++;
			return new Pair<Integer, Point>(highPieceID, new_position);
		} 
		else {
			Point curr_position = player_pieces.get(lowPieceID);
			Point new_position = new Point(curr_position);
			double theta = 0;
			double numAngles = 180;
			for(int i = 0; i <= numAngles; i++) {
				if(i % 2 == 0)
					theta -= i * Math.PI / numAngles;
				else
					theta += i * Math.PI / numAngles;
				double delta_x = diameter_piece * Math.cos(theta);
				double delta_y = diameter_piece * Math.sin(theta);

				new_position = isplayer1 ? new Point(curr_position.x - delta_x, curr_position.y + delta_y) : new Point(curr_position.x + delta_x, curr_position.y + delta_y);
				Pair<Integer, Point> move = new Pair<Integer, Point>(lowPieceID, new_position);

				Double dist = Board.getdist(player_pieces.get(move.getKey()), move.getValue());

				//System.out.println("         Aggressive, theta: " + theta);

				if(checkValidity(move, player_pieces, opponent_pieces, diameter_piece)) {
					player_pieces.put(lowPieceID, new_position);
					if((isplayer1 && new_position.x < -22.0) || (!isplayer1 && new_position.x > 22.0))
						this.RUNNER_STRATEGY_SET = false;
					return move;
				}
			}
			this.RUNNER_STRATEGY_SET = false;
		}
		return null;
	}
	
	public Integer getHighPieceID() {
		return highPieceID;
	}

	public Integer getLowPieceID() {
		return lowPieceID;
	}
	
	public HashMap<Integer, Point> getPlayerPieces() {
		return player_pieces;
	}

	public void setPlayerPieces(HashMap<Integer, Point> player_pieces) {
		this.player_pieces = player_pieces;
	}

	public HashMap<Integer, Point> getOpponentPieces() {
		return opponent_pieces;
	}

	public void setOpponentPieces(HashMap<Integer, Point> opponent_pieces) {
		this.opponent_pieces = opponent_pieces;
	}
	
	@Override
	public Pair<Integer, Point> getHybridMove() {
		Pair<Integer, Point> move = null; // TODO: Change this implementation
		return move;
	}
}