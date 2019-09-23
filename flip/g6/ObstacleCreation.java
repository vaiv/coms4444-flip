package flip.g6;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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
	private static final List<Double> WALL_POSITION_X = new ArrayList<>(Arrays.asList(-17.30, -13.84, -10.83, -6.92, -3.46, 0.00, 3.46, 6.92, 10.83, 13.84, 17.30));
	private ArrayList<Point> wall_pos_list = new ArrayList<Point>();  
	private ArrayList<Point> wall_pos_list_second = new ArrayList<Point>();  
	private HashMap<Integer, Point> wall_pieces = new HashMap<Integer, Point>();
	private HashMap<Integer, Point> wall_pieces_second = new HashMap<Integer, Point>();

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
		calculateWallPositionsMap(player_pieces);		
		List<Pair<Integer, Point>> possMoves = buildWall(player_pieces, opponent_pieces, wall_pieces);
		if(possMoves.size() != 0) {
			HashMap<Integer, Point> unfinished_pieces = getUnfinishedPlayerPieces(opponent_pieces, !isplayer1, Approach.AGGRESSIVE);
			HashMap<Integer, Point> closest_unfinished_pieces = getClosestPointsToOpponentBoundary(1, unfinished_pieces, !isplayer1);
			if(closest_unfinished_pieces.size() != 0) {
				for(Integer id : closest_unfinished_pieces.keySet()) {
					int possMoveID = -1;
					double distanceY = 10000000;
					int index = -1;
					for(int i = 0; i < possMoves.size(); i++) {
						Pair<Integer, Point> pair = possMoves.get(i);
						double newDistanceY = Math.sqrt(Math.pow(pair.getValue().y - closest_unfinished_pieces.get(id).y, 2));
						if(newDistanceY < distanceY) {
							possMoveID = pair.getKey();
							distanceY = newDistanceY;
							index = i;
						}
					}
					return possMoves.get(index);
				}
			}
			else {
				Random random = new Random();
				Integer piece_id = random.nextInt(possMoves.size());
				return possMoves.get(piece_id);
			}
		}
		else {
			possMoves = buildWall(player_pieces, opponent_pieces, wall_pieces_second);
			if(possMoves.size() != 0) {
				HashMap<Integer, Point> unfinished_pieces = getUnfinishedPlayerPieces(opponent_pieces, !isplayer1, Approach.AGGRESSIVE);
				HashMap<Integer, Point> closest_unfinished_pieces = getClosestPointsToOpponentBoundary(1, unfinished_pieces, !isplayer1);
				if(closest_unfinished_pieces.size() != 0) {
					for(Integer id : closest_unfinished_pieces.keySet()) {
						int possMoveID = -1;
						double distanceY = 10000000;
						int index = -1;
						for(int i = 0; i < possMoves.size(); i++) {
							Pair<Integer, Point> pair = possMoves.get(i);
							double newDistanceY = Math.sqrt(Math.pow(pair.getValue().y - closest_unfinished_pieces.get(id).y, 2));
							if(newDistanceY < distanceY) {
								possMoveID = pair.getKey();
								distanceY = newDistanceY;
								index = i;
							}
						}
						return possMoves.get(index);
					}
				}
				else {
					Random random = new Random();
					Integer piece_id = random.nextInt(possMoves.size());
					return possMoves.get(piece_id);
				}
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

		wall_pos_list = new ArrayList<Point>();
		wall_pos_list_second = new ArrayList<Point>();  
		
		for(int i = 0; i < WALL_POSITION_X.size(); i++){
			wall_pos_list.add(new Point(x1, WALL_POSITION_X.get(i)));
			wall_pos_list_second.add(new Point(x2, WALL_POSITION_X.get(i)));
		}
		
		for(Point p : wall_pos_list){
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
		
			for (Point point: points) {
				move = new Pair(id, point);
				if (checkValidity(move, player_pieces, opponent_pieces, diameter_piece))
					moves.add(move);
			}
		}
		return moves;		
	}

	public List<Point> moveCurrentToTarget(Integer id, Point current, Point target, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces) {
		List<Point> moves = new ArrayList<>();
		double tmcx = target.x-current.x;
		double tmcy = target.y-current.y;
		double d = Math.sqrt(tmcx*tmcx + tmcy*tmcy);
		double theta = Math.atan(tmcy/tmcx);
		if (d < EPSILON) {
			; // do nothing
		}
		if (d >= diameter_piece - EPSILON && d < diameter_piece + EPSILON) {
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
			if (checkValidity(next, player_pieces, opponent_pieces, diameter_piece)) {
				moves.add(m1);
				Point m2 = getNewPointFromOldPointAndAngle(m1, theta);
				moves.add(m2);
			} else {
				System.out.println("FAILED TO MOVE TO BLOCKADE POINT");
			}
		}
		return moves;
	}

	public Point getNewPointFromOldPointAndAngle(Point current, double theta) {
		Point new_position = new Point(current);
		double delta_x = diameter_piece * Math.cos(theta);
		double delta_y = diameter_piece * Math.sin(theta);
		new_position.x += this.isplayer1 ? -delta_x : delta_x;
		new_position.y += this.isplayer1 ? -delta_y : delta_y;
		return new_position;
	}

	private Integer nearestPoint(Point point, HashMap<Integer, Point> player_pieces){
		Integer id = 0;
		Point p;
		double d;
		double min_dist = Double.MAX_VALUE;
		for(int i = 0; i < n; i++){
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
}