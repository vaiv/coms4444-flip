package flip.g6;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Queue;
import java.util.Random;

import flip.g6.Move.Approach;
import flip.sim.Board;
import flip.sim.Point;
import javafx.util.Pair;

public class ObstacleCreation extends Move {

	private HashMap<Integer, Point> player_pieces;
	private HashMap<Integer, Point> opponent_pieces;
	private boolean isplayer1;
	private Integer n;
	private Double diameter_piece;

	// Wall point positions
	private static final double EPSILON = 0.001;
//	private static final List<Double> WALL_POSITION_Y = new ArrayList<>(Arrays.asList(-17.30, -13.84, -10.38, -6.92, -3.46, 0.00, 3.46, 6.92, 10.38, 13.84, 17.30));
	private static final List<Double> WALL1_POSITION_Y = new ArrayList<>(Arrays.asList(0.00, 3.46, -3.46, 6.92, -6.92, 10.38, -10.38, 13.84, -13.84, 17.30, -17.30));
	private static final List<Double> WALL2_POSITION_Y = new ArrayList<>(Arrays.asList(0.00, 3.46, -3.46, 6.92, -6.92, 10.38, -10.38, 13.84, -13.84, 17.30, -17.30));
	private ArrayList<Point> wall_pos_list = new ArrayList<Point>();
	private ArrayList<Point> wall_pos_list_second = new ArrayList<Point>();
	private HashMap<Integer, Point> wall_pieces = new HashMap<Integer, Point>();
	private HashMap<Integer, Point> wall_pieces_second = new HashMap<Integer, Point>();
	private Integer designated_wall_piece_id = -1;
	private Integer designated_wall2_piece_id = -1;
	private HashMap<Integer, Point> wall_points_done = new HashMap<>();
	private HashMap<Integer, Point> wall_points_on_deck = new HashMap<>();
	private HashMap<Integer, Point> wall2_points_done = new HashMap<>();
	private HashMap<Integer, Point> wall2_points_on_deck = new HashMap<>();
	private List<Integer> waiting_wall_pieces = new ArrayList<Integer>();

	public ObstacleCreation(HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, boolean isplayer1, Integer n, Double diameter_piece) {
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
		return true; // TODO: Change this implementation
	}

	@Override
	public Pair<Integer, Point> getMove() {
		System.out.println("Wall pieces on deck: " + wall_points_on_deck);
		System.out.println("Wall pieces done: " + wall_points_done);		
		System.out.println("Wall 2 pieces on deck: " + wall2_points_on_deck);
		System.out.println("Wall 2 pieces done: " + wall2_points_done);		
//		if(wall_pieces.size() != WALL_POSITION_Y.size() && wall_pieces_second.size() != WALL_POSITION_Y.size())
			calculateWallPositionsMap(player_pieces);
		List<Pair<Integer, Point>> possMoves = buildWall(player_pieces, opponent_pieces, wall_pieces);
//		System.out.println("Number of possible moves: " + possMoves.size());
		HashMap<Integer, Point> unfinished_opponent_pieces = getUnfinishedPlayerPieces(opponent_pieces, !isplayer1, Approach.AGGRESSIVE);
//		if(possMoves.size() == 0) {
//			calculateWallPositionsMap(player_pieces);
//			possMoves = buildWall(player_pieces, opponent_pieces, wall_pieces);
//		}
		
		if(possMoves.size() != 0 && unfinished_opponent_pieces.size() > 0 && wall_points_done.size() != WALL1_POSITION_Y.size()) {
			for(int i = 0; i < possMoves.size(); i++) {
				for(Integer wall_on_deck_id : wall_points_on_deck.keySet()) {
					if(wall_points_on_deck.size() > 0 && possMoves.get(i).getValue().equals(wall_points_on_deck.get(wall_on_deck_id))) {
						if(checkValidity(possMoves.get(i), player_pieces, opponent_pieces, diameter_piece)) {
							wall_points_done.put(wall_on_deck_id, wall_points_on_deck.get(wall_on_deck_id));
							wall_points_on_deck.remove(wall_on_deck_id);
							player_pieces.put(possMoves.get(i).getKey(), possMoves.get(i).getValue());						
							return possMoves.get(i);
						}
					}					
				}
			}
			HashMap<Integer, Point> closest_unfinished_opponent_pieces = getClosestPointsToOpponentBoundary(1, unfinished_opponent_pieces, !isplayer1);
			for(int i = 0; i < possMoves.size(); i++) {
				Pair<Integer, Point> move = possMoves.get(i);
				if(move.getKey().equals(designated_wall_piece_id)) {
					if(checkValidity(move, player_pieces, opponent_pieces, diameter_piece)) {
						player_pieces.put(designated_wall_piece_id, move.getValue());
						return move;
					}
					else {
						Integer collisionID = getWallCollisionSource(player_pieces, move);			
						for(int j = 0; j < possMoves.size(); j++) {
							Pair<Integer, Point> collisionMove = possMoves.get(j);
							if(collisionMove.getKey().equals(collisionID) && checkValidity(collisionMove, player_pieces, opponent_pieces, diameter_piece)) {
								designated_wall_piece_id = collisionID;
								player_pieces.put(collisionID, collisionMove.getValue());
								return collisionMove;
							}
						}
					}
					break;
				}
			}
			
			List<Pair<Integer, Point>> clonedPossMoves = new ArrayList<>();
			clonedPossMoves.addAll(possMoves);
			for(int i = 0; i < possMoves.size(); i++) {
				Pair<Integer, Point> move = possMoves.get(i);
				if(!checkValidity(move, player_pieces, opponent_pieces, diameter_piece))
					clonedPossMoves.remove(move);
			}
			possMoves = clonedPossMoves;			
			
			if(closest_unfinished_opponent_pieces.size() != 0) {
				for(Integer id : closest_unfinished_opponent_pieces.keySet()) {
					int possMoveID = -1;
					double distanceY = Double.MAX_VALUE;
					int index = -1;
					for(int i = 0; i < possMoves.size(); i++) {
						Pair<Integer, Point> possMove = possMoves.get(i);
						double newDistanceY = Math.abs(possMove.getValue().y - closest_unfinished_opponent_pieces.get(id).y);
						if(newDistanceY < distanceY) {
							possMoveID = possMove.getKey();
							distanceY = newDistanceY;
							index = i;
						}
					}
//					if((isplayer1 && closest_unfinished_opponent_pieces.get(id).x > -20) || (!isplayer1 && closest_unfinished_opponent_pieces.get(id).x < 20))
//						return possMoves.get(index);
					if(index != -1) {
						designated_wall_piece_id = possMoves.get(index).getKey();
						player_pieces.put(possMoves.get(index).getKey(), possMoves.get(index).getValue());
						return possMoves.get(index);
					}
				}
			}
			designated_wall_piece_id = possMoves.get(0).getKey();
			player_pieces.put(possMoves.get(0).getKey(), possMoves.get(0).getValue());
			return possMoves.get(0);
		}
		else {
			List<Pair<Integer, Point>> possMoves2 = buildWall(player_pieces, opponent_pieces, wall_pieces_second);
			if(possMoves2.size() != 0 && unfinished_opponent_pieces.size() > 0 && wall2_points_done.size() != WALL2_POSITION_Y.size()){
				for(int i = 0; i < possMoves2.size(); i++) {
					for(Integer wall_on_deck_id : wall2_points_on_deck.keySet()) {
						if(wall2_points_on_deck.size() > 0 && possMoves2.get(i).getValue().equals(wall2_points_on_deck.get(wall_on_deck_id))) {
							if(checkValidity(possMoves2.get(i), player_pieces, opponent_pieces, diameter_piece)) {
								wall2_points_done.put(wall_on_deck_id, wall2_points_on_deck.get(wall_on_deck_id));
								wall2_points_on_deck.remove(wall_on_deck_id);
								player_pieces.put(possMoves2.get(i).getKey(), possMoves2.get(i).getValue());						
								return possMoves2.get(i);
							}
						}					
					}
				}
				HashMap<Integer, Point> closest_unfinished_opponent_pieces = getClosestPointsToOpponentBoundary(1, unfinished_opponent_pieces, !isplayer1);
				for(int i = 0; i < possMoves2.size(); i++) {
					Pair<Integer, Point> move = possMoves2.get(i);
					if(move.getKey().equals(designated_wall2_piece_id)) {
						if(checkValidity(move, player_pieces, opponent_pieces, diameter_piece)) {
							player_pieces.put(designated_wall2_piece_id, move.getValue());
							return move;
						}
						else {
							Integer collisionID = getWallCollisionSource(player_pieces, move);			
							for(int j = 0; j < possMoves2.size(); j++) {
								Pair<Integer, Point> collisionMove = possMoves2.get(j);
								if(collisionMove.getKey().equals(collisionID) && checkValidity(collisionMove, player_pieces, opponent_pieces, diameter_piece)) {
									designated_wall2_piece_id = collisionID;
									player_pieces.put(collisionID, collisionMove.getValue());
									return collisionMove;
								}
							}
						}
						break;
					}
				}
				
				List<Pair<Integer, Point>> clonedpossMoves2 = new ArrayList<>();
				clonedpossMoves2.addAll(possMoves2);
				for(int i = 0; i < possMoves2.size(); i++) {
					Pair<Integer, Point> move = possMoves2.get(i);
					if(!checkValidity(move, player_pieces, opponent_pieces, diameter_piece))
						clonedpossMoves2.remove(move);
				}
				possMoves2 = clonedpossMoves2;			
				
				if(closest_unfinished_opponent_pieces.size() != 0) {
					for(Integer id : closest_unfinished_opponent_pieces.keySet()) {
						int possMoveID = -1;
						double distanceY = Double.MAX_VALUE;
						int index = -1;
						for(int i = 0; i < possMoves2.size(); i++) {
							Pair<Integer, Point> possMove = possMoves2.get(i);
							double newDistanceY = Math.abs(possMove.getValue().y - closest_unfinished_opponent_pieces.get(id).y);
							if(newDistanceY < distanceY) {
								possMoveID = possMove.getKey();
								distanceY = newDistanceY;
								index = i;
							}
						}
//						if((isplayer1 && closest_unfinished_opponent_pieces.get(id).x > -20) || (!isplayer1 && closest_unfinished_opponent_pieces.get(id).x < 20))
//							return possMoves2.get(index);
						if(index != -1) {
							designated_wall2_piece_id = possMoves2.get(index).getKey();
							player_pieces.put(possMoves2.get(index).getKey(), possMoves2.get(index).getValue());
							return possMoves2.get(index);
						}
					}
				}
				designated_wall2_piece_id = possMoves2.get(0).getKey();
				player_pieces.put(possMoves2.get(0).getKey(), possMoves2.get(0).getValue());
				return possMoves2.get(0);
			}
		}

		return null;

		//		Pair<Integer, Point> move = null; // TODO: Change this implementation
		//
		//		HashMap<Integer, Point> unfinished_pieces = getUnfinishedPlayerPieces(player_pieces, isplayer1, Approach.CREATION);
		//		HashMap<Integer, Point> closest_pieces = getClosestPointsToOpponentBoundary(n / 2, player_pieces, isplayer1);
		//		HashMap<Integer, Point> relevant_pieces = (HashMap<Integer, Point>) player_pieces.clone();
		//		HashMap<Integer, Point> relevant_unfinished_pieces = new HashMap<>();		
		//		
		//		for(Integer id : closest_pieces.keySet()) {
		//			relevant_pieces.remove(id);
		//		}
		//		
		//		for(Integer id : relevant_pieces.keySet())
		//			if(unfinished_pieces.containsKey(id) && relevant_pieces.containsKey(id))
		//				relevant_unfinished_pieces.put(id, relevant_pieces.get(id));		
		//		
		//		//System.out.println("Number of OC unfinished pieces: " + unfinished_pieces.size());
		//
		//		Random random = new Random();
		//		Integer piece_id = random.nextInt(n);
		//		while(!relevant_unfinished_pieces.containsKey(piece_id))
		//			piece_id = random.nextInt(n);
		//		Point curr_position = player_pieces.get(piece_id);
		//		Point new_position = new Point(curr_position);
		//
		////		double maxInteriorDistance = 0;
		////		if(isplayer1) {
		////			maxInteriorDistance = 20;
		////		}else {
		////			maxInteriorDistance = -20;
		////		}
		//
		////		if((isplayer1 && curr_position.x < maxInteriorDistance) || (!isplayer1 && curr_position.x > maxInteriorDistance)) {
		//			// Player 1
		////			try {
		////				return getMove();
		////				return null;
		////			} catch (Exception e) {
		//				// TODO Auto-generated catch block
		////				return null;
		////			}
		////		}
		//
		//		double theta = -Math.PI/2 + Math.PI * random.nextDouble();
		//		double delta_x = 0;
		//		double delta_y = 0;
		//		delta_x = diameter_piece * Math.cos(theta);
		//		delta_y = diameter_piece * Math.sin(theta);
		//
		//		Double val = (Math.pow(delta_x,2) + Math.pow(delta_y, 2));
		//		// System.out.println("delta_x^2 + delta_y^2 = " + val.toString() + " theta values are " + Math.cos(theta) + " " + Math.sin(theta) + " diameter is " + diameter_piece);
		//		// Log.record("delta_x^2 + delta_y^2 = " + val.toString() + " theta values are " + Math.cos(theta) + " " + Math.sin(theta) + " diameter is " + diameter_piece);
		//
		//		new_position.x = isplayer1 ? new_position.x - delta_x : new_position.x + delta_x;
		//		new_position.y += delta_y;
		//		move = new Pair<Integer, Point>(piece_id, new_position);
		//
		//		Double dist = Board.getdist(player_pieces.get(move.getKey()), move.getValue());
		//		// System.out.println("distance from previous position is " + dist.toString());
		//		// Log.record("distance from previous position is " + dist.toString());
		//		
		//		return move;
	}

	@Override
	public Pair<Integer, Point> getHybridMove() {
		Pair<Integer, Point> move = null; // TODO: Change this implementation
		return move;
	}

	public void calculateWallPositionsMap(HashMap<Integer, Point> player_pieces) {

		double x1 = this.isplayer1 ? 19.99 : -19.99;
		double x2 = this.isplayer1 ? 22.01 : -22.01;
		
		wall_pieces = new HashMap<>();
		wall_pieces_second = new HashMap<>();

		wall_pos_list = new ArrayList<Point>();
		wall_pos_list_second = new ArrayList<Point>();  
		
		for(int i = 0; i < WALL1_POSITION_Y.size(); i++) {
			boolean wall_position_done = false;
			for(Integer j : wall_points_done.keySet()) {
				if(WALL1_POSITION_Y.get(i).equals(wall_points_done.get(j).y)) {
					wall_position_done = true;
					break;
				}
			}
			if(wall_position_done)
				continue;
			wall_pos_list.add(new Point(x1, WALL1_POSITION_Y.get(i)));
		}
		
		System.out.println("Remaining wall positions: " + wall_pos_list);

		for(int i = 0; i < WALL2_POSITION_Y.size(); i++){
			boolean wall2_position_done = false;
			for(Integer j : wall2_points_done.keySet()) {
				if(WALL2_POSITION_Y.get(i).equals(wall2_points_done.get(j).y)) {
					wall2_position_done = true;
					break;
				}
			}
			if(wall2_position_done)
				continue;
			wall_pos_list_second.add(new Point(x2, WALL2_POSITION_Y.get(i)));
		}
				
		for(Point p : wall_pos_list) {
			Integer id = nearestPoint(p, player_pieces);
			wall_pieces.put(id, p);
		}
		
		for(Point p : wall_pos_list_second) {
			Integer id = nearestPoint(p, player_pieces);
			wall_pieces_second.put(id, p);
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
				move = new Pair(id, point);
//				if (checkValidity(move, player_pieces, opponent_pieces, diameter_piece))
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
			return moves; // do nothing
		}
		if (d >= diameter_piece - EPSILON && d < diameter_piece + EPSILON) {
			wall_points_on_deck.put(id, target);
			moves.add(target);
		} else if (d > EPSILON && d <= 2*diameter_piece) {
			moves.addAll(moveCurrentToTargetClose(new Pair<Integer, Point>(id, current), target, player_pieces, opponent_pieces));
			if (moves.isEmpty()) {
				Point behind_current = new Point(current.x, current.y);
				behind_current.x += isplayer1 ? diameter_piece : -diameter_piece;
				moves.add(behind_current);
			}
		} else if (d > 2*diameter_piece && d <= 3*diameter_piece) {
			Point new_position = getNewPointFromOldPointAndAngle(current, theta);
			moves.add(new_position);
			moves.addAll(moveCurrentToTargetClose(new Pair<Integer, Point>(id, new_position), target, player_pieces, opponent_pieces));
			if (moves.size() == 1) {
				Point behind_current = new Point(current.x, current.y);
				behind_current.x += isplayer1 ? diameter_piece : -diameter_piece;
				moves.add(behind_current);
			}
		} else if (d > 3*diameter_piece) {
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
		// We need to solve for a 2-move sequence that gets the current point to the target
		double tmcx2 = tmcx/2;
		double tmcy2 = tmcy/2;
		// tpp2 is (theta + phi)/2
		double tpp2 = Math.atan(tmcy/tmcx);
		// tmp2 is (theta - phi)/2
		double tmp2 = Math.acos(Math.sqrt(tmcx2*tmcx2 + tmcy2*tmcy2)/2);
		double theta = tpp2 + tmp2;
		double phi = tpp2 - tmp2;
		// if you are blocked, take the other angle first
		// if that still doesn't work, move to the point directly behind the current spot
		Point m1 = getNewPointFromOldPointAndAngle(current_point, theta);
		Pair<Integer, Point> next = new Pair(current_id, m1);
		if (checkValidity(next, player_pieces, opponent_pieces, diameter_piece)) {
			moves.add(m1);
			Point m2 = getNewPointFromOldPointAndAngle(m1, phi);
			moves.add(m2);
		} else {
			m1 = getNewPointFromOldPointAndAngle(current_point, phi);
			next = new Pair(current_id, m1);
			if (checkValidity(next, player_pieces, opponent_pieces, diameter_piece)) {
				moves.add(m1);
				Point m2 = getNewPointFromOldPointAndAngle(m1, theta);
				moves.add(m2);
			} else {
				theta = 0;
				double numAngles = 270;
				for(int i = 0; i <= numAngles; i++) {
					if(i % 2 == 0)
						theta -= i * 3 * Math.PI / (2 * numAngles);
					else
						theta += i * 3 * Math.PI / (2 * numAngles);
					double delta_x = diameter_piece * Math.cos(theta);
					double delta_y = diameter_piece * Math.sin(theta);

					m1 = isplayer1 ? new Point(current_point.x - delta_x, current_point.y + delta_y) : new Point(current_point.x + delta_x, current_point.y + delta_y);
					next = new Pair(current_id, m1);
					if(checkValidity(next, player_pieces, opponent_pieces, diameter_piece)) {
						//System.out.println("THETA USED...Aggressive, theta: " + theta);
						System.out.println("Making any possible move at this point...");
//						System.out.println("FAILED TO MOVE TO BLOCKADE POINT");
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
		double delta_x = diameter_piece * Math.cos(theta);
		double delta_y = diameter_piece * Math.sin(theta);
		double x = current.x + (this.isplayer1 ? -delta_x : delta_x);
		double y = current.y + (this.isplayer1 ? -delta_y : delta_y);
		new_position = new Point(x, y);
		return new_position;
	}

	private Integer nearestPoint(Point point, HashMap<Integer, Point> player_pieces){
		Integer id = 0;
		Point p;
		double d;
		double min_dist = Double.MAX_VALUE;
		for(Integer i : player_pieces.keySet()) {
			boolean player_piece_built_on_wall = false;
			for(Integer j : wall_points_done.keySet()) {
				if(i.equals(j)) {
					player_piece_built_on_wall = true;
					break;
				}
			}
			if(player_piece_built_on_wall)
				continue;
			boolean player_piece_built_on_wall2 = false;
			for(Integer j : wall2_points_done.keySet()) {
				if(i.equals(j)) {
					player_piece_built_on_wall2 = true;
					break;
				}
			}
			if(player_piece_built_on_wall2)
				continue;
			if(wall_pieces.containsKey(i) || wall_pieces_second.containsKey(i))
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
            if(Board.getdist(piece.getValue(), entry.getValue()) + 1E-7 < diameter_piece) {
                return entry.getKey();
            }
        }
        return -1;
    }
}
