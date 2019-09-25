package flip.g6;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import flip.sim.Board;
import flip.sim.Point;
import javafx.util.Pair;

public class ObstacleCreation extends Move {

	private HashMap<Integer, Point> playerPieces;
	private HashMap<Integer, Point> opponentPieces;
	private boolean isPlayer1;
	private Double diameterPiece;

	// Wall point positions
	private static final double EPSILON = 0.001;
	private static final List<Double> WALL_POSITION_Y = new ArrayList<>(Arrays.asList(0.00, 3.46, -3.46, 6.92, -6.92, 10.38, -10.38, 13.84, -13.84, 17.30, -17.30));
	private ArrayList<Point> wall1PosList = new ArrayList<Point>();
	private ArrayList<Point> wall2PosList = new ArrayList<Point>();
	private HashMap<Integer, Point> wall1PiecesLeft = new HashMap<Integer, Point>();
	private HashMap<Integer, Point> wall2PiecesLeft = new HashMap<Integer, Point>();
	private Integer designatedWall1PieceID = -1;
	private Integer designatedWall2PieceID = -1;
	private HashMap<Integer, Point> wall1PointsDone = new HashMap<>();
	private HashMap<Integer, Point> wall1PointsOnDeck = new HashMap<>();
	private HashMap<Integer, Point> wall2PointsDone = new HashMap<>();
	private HashMap<Integer, Point> wall2PointsOnDeck = new HashMap<>();

	public ObstacleCreation(HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, boolean isplayer1, Integer n, Double diameter_piece) {
		this.playerPieces = player_pieces;
		this.opponentPieces = opponent_pieces;
		this.isPlayer1 = isplayer1;
		this.diameterPiece = diameter_piece;
	}

	@Override
	public void updatePieceInfo(HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces) {
		this.playerPieces = player_pieces;
		this.opponentPieces = opponent_pieces;
	}

	@Override
	public Pair<Integer, Point> getMove() {
		calculateWallPositionsMap(playerPieces);
		List<Pair<Integer, Point>> possMoves = buildWall(playerPieces, opponentPieces, wall1PiecesLeft);
		HashMap<Integer, Point> unfinished_opponent_pieces = getUnfinishedPlayerPieces(opponentPieces, !isPlayer1, Approach.AGGRESSIVE);

		if(possMoves.size() != 0 && unfinished_opponent_pieces.size() > 0 && wall1PointsDone.size() != WALL_POSITION_Y.size()) {
			for(int i = 0; i < possMoves.size(); i++) {
				for(Integer wall_on_deck_id : wall1PointsOnDeck.keySet()) {
					if(wall1PointsOnDeck.size() > 0 && possMoves.get(i).getValue().equals(wall1PointsOnDeck.get(wall_on_deck_id))) {
						if(checkValidity(possMoves.get(i), playerPieces, opponentPieces, diameterPiece)) {
							wall1PointsDone.put(wall_on_deck_id, wall1PointsOnDeck.get(wall_on_deck_id));
							wall1PointsOnDeck.remove(wall_on_deck_id);
							playerPieces.put(possMoves.get(i).getKey(), possMoves.get(i).getValue());						
							return possMoves.get(i);
						}
					}
				}
			}
			HashMap<Integer, Point> closest_unfinished_opponent_pieces = getClosestPointsToOpponentBoundary(1, unfinished_opponent_pieces, !isPlayer1);
			for(int i = 0; i < possMoves.size(); i++) {
				Pair<Integer, Point> move = possMoves.get(i);
				if(move.getKey().equals(designatedWall1PieceID)) {
					if(checkValidity(move, playerPieces, opponentPieces, diameterPiece)) {
						playerPieces.put(designatedWall1PieceID, move.getValue());
						return move;
					}
				}
			}

			List<Pair<Integer, Point>> clonedPossMoves = new ArrayList<>();
			clonedPossMoves.addAll(possMoves);
			for(int i = 0; i < possMoves.size(); i++) {
				Pair<Integer, Point> move = possMoves.get(i);
				if(!checkValidity(move, playerPieces, opponentPieces, diameterPiece))
					clonedPossMoves.remove(move);
			}
			possMoves = clonedPossMoves;			

			if(closest_unfinished_opponent_pieces.size() != 0) {
				for(Integer id : closest_unfinished_opponent_pieces.keySet()) {
					double distanceY = Double.MAX_VALUE;
					int index = -1;
					for(int i = 0; i < possMoves.size(); i++) {
						Pair<Integer, Point> possMove = possMoves.get(i);
						double newDistanceY = Math.abs(possMove.getValue().y - closest_unfinished_opponent_pieces.get(id).y);
						if(newDistanceY < distanceY) {
							distanceY = newDistanceY;
							index = i;
						}
					}
					if(index != -1) {
						designatedWall1PieceID = possMoves.get(index).getKey();
						playerPieces.put(possMoves.get(index).getKey(), possMoves.get(index).getValue());
						return possMoves.get(index);
					}
				}
			}
			designatedWall1PieceID = possMoves.get(0).getKey();
			playerPieces.put(possMoves.get(0).getKey(), possMoves.get(0).getValue());
			return possMoves.get(0);
		}
		List<Pair<Integer, Point>> possMoves2 = buildWall(playerPieces, opponentPieces, wall2PiecesLeft);
		if(possMoves2.size() != 0 && unfinished_opponent_pieces.size() > 0 && wall2PointsDone.size() != WALL_POSITION_Y.size()) {
			for(int i = 0; i < possMoves2.size(); i++) {
				for(Integer wall_on_deck_id : wall2PointsOnDeck.keySet()) {
					if(wall2PointsOnDeck.size() > 0 && possMoves2.get(i).getValue().equals(wall2PointsOnDeck.get(wall_on_deck_id))) {
						if(checkValidity(possMoves2.get(i), playerPieces, opponentPieces, diameterPiece)) {
							wall2PointsDone.put(wall_on_deck_id, wall2PointsOnDeck.get(wall_on_deck_id));
							wall2PointsOnDeck.remove(wall_on_deck_id);
							playerPieces.put(possMoves2.get(i).getKey(), possMoves2.get(i).getValue());						
							return possMoves2.get(i);
						}
					}					
				}
			}
			HashMap<Integer, Point> closest_unfinished_opponent_pieces = getClosestPointsToOpponentBoundary(1, unfinished_opponent_pieces, !isPlayer1);
			for(int i = 0; i < possMoves2.size(); i++) {
				Pair<Integer, Point> move = possMoves2.get(i);
				if(move.getKey().equals(designatedWall2PieceID)) {
					if(checkValidity(move, playerPieces, opponentPieces, diameterPiece)) {
						playerPieces.put(designatedWall2PieceID, move.getValue());
						return move;
					}
					else {
						Integer collisionID = getWallCollisionSource(playerPieces, move);			
						for(int j = 0; j < possMoves2.size(); j++) {
							Pair<Integer, Point> collisionMove = possMoves2.get(j);
							if(collisionMove.getKey().equals(collisionID) && checkValidity(collisionMove, playerPieces, opponentPieces, diameterPiece)) {
								designatedWall2PieceID = collisionID;
								playerPieces.put(collisionID, collisionMove.getValue());
								return collisionMove;
							}
						}
					}
					break;
				}
			}

			List<Pair<Integer, Point>> clonedPossMoves2 = new ArrayList<>();
			clonedPossMoves2.addAll(possMoves2);
			for(int i = 0; i < possMoves2.size(); i++) {
				Pair<Integer, Point> move = possMoves2.get(i);
				if(!checkValidity(move, playerPieces, opponentPieces, diameterPiece))
					clonedPossMoves2.remove(move);
			}
			possMoves2 = clonedPossMoves2;			

			if(closest_unfinished_opponent_pieces.size() != 0) {
				for(Integer id : closest_unfinished_opponent_pieces.keySet()) {
					double distanceY = Double.MAX_VALUE;
					int index = -1;
					for(int i = 0; i < possMoves2.size(); i++) {
						Pair<Integer, Point> possMove = possMoves2.get(i);
						double newDistanceY = Math.abs(possMove.getValue().y - closest_unfinished_opponent_pieces.get(id).y);
						if(newDistanceY < distanceY) {
							distanceY = newDistanceY;
							index = i;
						}
					}
					if(index != -1) {
						designatedWall2PieceID = possMoves2.get(index).getKey();
						playerPieces.put(possMoves2.get(index).getKey(), possMoves2.get(index).getValue());
						return possMoves2.get(index);
					}
				}
			}
			designatedWall2PieceID = possMoves2.get(0).getKey();
			playerPieces.put(possMoves2.get(0).getKey(), possMoves2.get(0).getValue());
			return possMoves2.get(0);
		}

		return null;
	}

	public void calculateWallPositionsMap(HashMap<Integer, Point> player_pieces) {

		double x1 = this.isPlayer1 ? 19.99 : -19.99;
		double x2 = this.isPlayer1 ? 22.01 : -22.01;

		wall1PiecesLeft = new HashMap<>();
		wall2PiecesLeft = new HashMap<>();

		wall1PosList = new ArrayList<Point>();
		wall2PosList = new ArrayList<Point>();  

		for(int i = 0; i < WALL_POSITION_Y.size(); i++) {
			boolean wall_position_done = false;
			for(Integer j : wall1PointsDone.keySet()) {
				if(WALL_POSITION_Y.get(i).equals(wall1PointsDone.get(j).y)) {
					wall_position_done = true;
					break;
				}
			}
			if(wall_position_done)
				continue;
						x1 += isPlayer1 ? 0.22 : -0.22;
			wall1PosList.add(new Point(x1, WALL_POSITION_Y.get(i)));
		}

		for(int i = 0; i < WALL_POSITION_Y.size(); i++){
			boolean wall2_position_done = false;
			for(Integer j : wall2PointsDone.keySet()) {
				if(WALL_POSITION_Y.get(i).equals(wall2PointsDone.get(j).y)) {
					wall2_position_done = true;
					break;
				}
			}
			if(wall2_position_done)
				continue;
						x2 += isPlayer1 ? 0.22 : -0.22;
			wall2PosList.add(new Point(x2, WALL_POSITION_Y.get(i)));
		}

		for(Point p : wall1PosList) {
			Integer id = nearestPoint(p, player_pieces);
			if(id != -1)
				wall1PiecesLeft.put(id, p);
		}

		for(Point p : wall2PosList) {
			Integer id = nearestPoint(p, player_pieces);
			if(id != -1)
				wall2PiecesLeft.put(id, p);
		}
	}

	public List<Pair<Integer, Point>> buildWall(HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, HashMap<Integer, Point> wall) {
		Integer id;
		Point destination;
		Pair<Integer, Point> move;
		List<Pair<Integer, Point>> moves = new ArrayList<>();

		for (HashMap.Entry<Integer, Point> entry : wall.entrySet()) {
			id = entry.getKey();
			destination = entry.getValue();

			List<Point> points = moveCurrentToTarget(id, player_pieces.get(id), destination, player_pieces, opponent_pieces);

			for (Point point : points) {
				move = new Pair<Integer, Point>(id, point);
				moves.add(move);
			}
		}
		return moves;
	}

	public List<Point> moveCurrentToTarget(Integer id, Point current, Point target, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces) {
		List<Point> moves = new ArrayList<>();
		double tmcx = target.x - current.x;
		double tmcy = target.y - current.y;
		double d = Math.sqrt(tmcx * tmcx + tmcy * tmcy);
		double theta = Math.atan(tmcy/tmcx);
		if (d < EPSILON) {
			return moves;
		}
		if (d >= diameterPiece - EPSILON && d < diameterPiece + EPSILON) {
			if(wall1PiecesLeft.containsKey(id))
				wall1PointsOnDeck.put(id, target);
			else if(wall2PiecesLeft.containsKey(id))
				wall2PointsOnDeck.put(id, target);
			moves.add(target);
		} else if (d > EPSILON && d <= 2*diameterPiece) {
			moves.addAll(moveCurrentToTargetClose(new Pair<Integer, Point>(id, current), target, player_pieces, opponent_pieces));
			if (moves.isEmpty()) {
				Point behind_current = new Point(current.x, current.y);
				behind_current.x += isPlayer1 ? diameterPiece : -diameterPiece;
				moves.add(behind_current);
			}
		} else if (d > 2*diameterPiece && d <= 3 * diameterPiece) {
			Point new_position = getNewPointFromOldPointAndAngle(current, theta);
			moves.add(new_position);
			moves.addAll(moveCurrentToTargetClose(new Pair<Integer, Point>(id, new_position), target, player_pieces, opponent_pieces));
			if (moves.size() == 1) {
				Point behind_current = new Point(current.x, current.y);
				behind_current.x += isPlayer1 ? diameterPiece : -diameterPiece;
				moves.add(behind_current);
			}
		} else if (d > 3 * diameterPiece) {
			Point m1 = getNewPointFromOldPointAndAngle(current, theta);
			moves.add(m1);
			Point m2 = getNewPointFromOldPointAndAngle(m1, theta);
			moves.add(m2);
		}
		return moves;
	}

	public List<Point> moveCurrentToTargetClose(Pair<Integer, Point> current, Point target, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces) {
		List<Point> moves = new ArrayList<>();
		Integer current_id = current.getKey();
		Point current_point = current.getValue();
		double tmcx = target.x-current_point.x;
		double tmcy = target.y-current_point.y;
		double tmcx2 = tmcx/2;
		double tmcy2 = tmcy/2;
		double tpp2 = Math.atan(tmcy/tmcx);
		double tmp2 = Math.acos(Math.sqrt(tmcx2*tmcx2 + tmcy2*tmcy2)/2);
		double theta = tpp2 + tmp2;
		double phi = tpp2 - tmp2;

		Point m1 = getNewPointFromOldPointAndAngle(current_point, theta);
		Pair<Integer, Point> next = new Pair<Integer, Point>(current_id, m1);
		if (checkValidity(next, player_pieces, opponent_pieces, diameterPiece)) {
			moves.add(m1);
			Point m2 = getNewPointFromOldPointAndAngle(m1, phi);
			moves.add(m2);
		} else {
			m1 = getNewPointFromOldPointAndAngle(current_point, phi);
			next = new Pair<Integer, Point>(current_id, m1);
			if (checkValidity(next, player_pieces, opponent_pieces, diameterPiece)) {
				moves.add(m1);
				Point m2 = getNewPointFromOldPointAndAngle(m1, theta);
				moves.add(m2);
			} else {
				theta = 0;
				double numAngles = 180;
				for(int i = 0; i <= numAngles; i++) {
					if(i % 2 == 0)
						theta -= i * Math.PI / numAngles;
					else
						theta += i * Math.PI / numAngles;
					double delta_x = diameterPiece * Math.cos(theta);
					double delta_y = diameterPiece * Math.sin(theta);

					m1 = isPlayer1 ? new Point(current_point.x - delta_x, current_point.y + delta_y) : new Point(current_point.x + delta_x, current_point.y + delta_y);
					next = new Pair<Integer, Point>(current_id, m1);
					if(checkValidity(next, player_pieces, opponent_pieces, diameterPiece)) {
						moves.add(m1);
						break;
					}
				}
			}
		}
		return moves;
	}

	public Point getNewPointFromOldPointAndAngle(Point current, double theta) {
		Point new_position;
		double delta_x = diameterPiece * Math.cos(theta);
		double delta_y = diameterPiece * Math.sin(theta);
		double x = current.x + (this.isPlayer1 ? -delta_x : delta_x);
		double y = current.y + (this.isPlayer1 ? -delta_y : delta_y);
		new_position = new Point(x, y);
		return new_position;
	}

	private Integer nearestPoint(Point point, HashMap<Integer, Point> player_pieces){
		Integer id = -1;
		Point p;
		double d;
		double min_dist = Double.MAX_VALUE;
		for(Integer i : player_pieces.keySet()) {
			if(wall1PointsDone.containsKey(i) || wall2PointsDone.containsKey(i))
				continue;
			if(wall1PiecesLeft.containsKey(i) || wall2PiecesLeft.containsKey(i))
				continue;

			p = player_pieces.get(i);
			d = Math.sqrt(Math.pow(point.x - p.x, 2) + Math.pow(point.y - p.y, 2));
			if(d < min_dist){
				min_dist = d;
				id = i;
			}				
		}
		return id;
	}

	public Integer getWallCollisionSource(HashMap<Integer, Point> player_pieces, Pair<Integer, Point> piece) {
		for(HashMap.Entry<Integer, Point> entry : player_pieces.entrySet()) {
			if(Board.getdist(piece.getValue(), entry.getValue()) + 1E-7 < diameterPiece) {
				return entry.getKey();
			}
		}
		return -1;
	}

	public List<Pair<Integer, Point>> doubleWallSwap(){
		List<Pair<Integer, Point>> moves = new ArrayList<>();
		Point p1, p2;

		for (Integer id1 : wall1PointsDone.keySet()) {
			p1 = wall1PointsDone.get(id1); 

			for (Integer id2 : wall2PointsDone.keySet()) {
				p2 = wall2PointsDone.get(id2);
				if(Math.abs(p1.y - p2.y) < 0.1) {				
					Point destination;
					Pair<Integer, Point> move;

					double numAngles = 180;
					double theta = 0;
					for(int i = 0; i <= numAngles; i++) {
						if(i % 2 == 0)
							theta -= i * Math.PI / numAngles;
						else
							theta += i * Math.PI / numAngles;
						double delta_x = diameterPiece * Math.cos(theta);
						double delta_y = diameterPiece * Math.sin(theta);

						destination = new Point(p1.x + (isPlayer1 ? -delta_x : delta_x), p1.y + delta_y);
						move = new Pair<Integer, Point>(id1, destination);
						if (checkValidity(move, playerPieces, opponentPieces, diameterPiece)) {
							
							// Point 1
							moves.add(move);
							playerPieces.put(id1, destination);

							// Point 2
							move = new Pair<Integer, Point>(id2, new Point(p2.x + (isPlayer1 ? -diameterPiece : diameterPiece), p2.y));
							moves.add(move);
							playerPieces.put(id2, p1);

							return moves;
						}
					}
				}
			}
		}
		return null;
	}
	
	public HashMap<Integer, Point> getPlayerPieces() {
		return playerPieces;
	}

	public void setPlayerPieces(HashMap<Integer, Point> playerPieces) {
		this.playerPieces = playerPieces;
	}

	public HashMap<Integer, Point> getOpponentPieces() {
		return opponentPieces;
	}

	public void setOpponentPieces(HashMap<Integer, Point> opponentPieces) {
		this.opponentPieces = opponentPieces;
	}
}