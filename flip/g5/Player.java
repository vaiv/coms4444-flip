package flip.g5;
import java.util.*;
import java.util.stream.Collectors;
import javafx.util.Pair; 

import flip.sim.Point;
import flip.sim.Board;
import flip.sim.Log;

public class Player implements flip.sim.Player
{
	private int seed = 42;
	private Random random;
	private boolean isplayer1;
	private Integer n;
	private Double diameter_piece;

	private static final double EPSILON = 0.001;
	private static final double B1  = -17.30;
	private static final double B2  = 17.30;
	private static final double B3  = -13.84;
	private static final double B4  = 13.84;
	private static final double B5  = -10.38;
	private static final double B6  = 10.38;
	private static final double B7  = -6.92;
	private static final double B8  = 6.92;
	private static final double B9  = -3.46;
	private static final double B10 = 3.46;
	private static final double B11 = 0.00;

	private static final List<Double> BLOCKADE_YCOORD = new ArrayList<>(Arrays.asList(B1, B2, B3, B4, B5, B6, B7, B8, B9, B10, B11));
	private List<Point> blockadeList = new ArrayList<>();
	private Map<Point, Integer> blockadeMap = new HashMap<>();

	public Player()
	{
		random = new Random(seed);
	}

	// Initialization function.
    // pieces: Location of the pieces for the player.
    // n: Number of pieces available.
    // t: Total turns available.
	public void init(HashMap<Integer, Point> pieces, int n, double t, boolean isplayer1, double diameter_piece)
	{
		this.n = n;
		this.isplayer1 = isplayer1;
		this.diameter_piece = diameter_piece;
		this.initializeBlockadeList(isplayer1);
		computeBlockadeMap(pieces, isplayer1);
	}

	public List<Pair<Integer, Point>> getMoves(Integer num_moves, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, boolean isplayer1)
	{
		this.removeOccupiedBlockadePoints(player_pieces);
		List<Point> blockadeImportance = this.computeBlockadeImportance(opponent_pieces, isplayer1);
		return getMovesForBlockade(blockadeImportance, player_pieces, opponent_pieces, isplayer1);
	}

	private List<Pair<Integer, Point>> getMovesForBlockade(List<Point> blockade, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, boolean isplayer1) {
		if (this.n < 11) return new ArrayList<>();

		List<Pair<Integer, Point>> moves = new ArrayList<>();

		// Match pieces to blockade points from top to bottom !!! (as an approximation)
		// Compute the minimum weight bipartite matching for a better solution
		for (Point target : blockade) {
			// Find the nearest piece behind the blockade and move it towards the blockade point
			Integer id = this.blockadeMap.get(target);
			Point current = player_pieces.get(id);
			System.out.println("Start: " + " x: " + current.x + " y: " + current.y);
			System.out.println("Target: " + " x: " + target.x + " y: " + target.y);
			List<Point> points = moveCurrentToTarget(id, current, target, player_pieces, opponent_pieces);
			for (Point point: points) {
				Pair<Integer, Point> move = new Pair(id, point);
				System.out.println("Update: " + " x: " + point.x + " y: " + point.y);
				if (check_validity(move, player_pieces, opponent_pieces)) {
					System.out.println("Valid!");
					moves.add(move);
					player_pieces.put(id, point);
				} else {
					System.out.println("Invalid!");
					break;
				}
			}
			if (moves.size() >= 2) break;
		}
		return moves;
	}

	// private List<Pair<Integer, Point>> getMovesForBlockade(HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, boolean isplayer1) {
	// 	if (n >= 11) {
	// 		List<Pair<Integer, Point>> moves = new ArrayList<>();
	// 		Set<Integer> finished = new HashSet<>();
	// 		for (Map.Entry<Integer, Point> entry: blockadeMap.entrySet()) {
	// 			Point current = player_pieces.get(entry.getKey());
	// 			Point target = entry.getValue();
	// 			double d = getDistance(current, target);
	// 			if (d < EPSILON) {
	// 				finished.add(entry.getKey());
	// 			}
	// 		}
	// 		for (Integer i: finished) {
	// 			blockadeMap.remove(i);
	// 		}

	// 		for (Map.Entry<Integer, Point> entry: blockadeMap.entrySet()) {
	// 			System.out.println("Start: " + " x: " + player_pieces.get(entry.getKey()).x +
	// 				" y: " + player_pieces.get(entry.getKey()).y);
	// 			System.out.println("Target: " + " x: " + entry.getValue().x +
	// 				" y: " + entry.getValue().y);
	// 			List<Point> points = moveCurrentToTarget(player_pieces.get(entry.getKey()), entry.getValue());
	// 			for (Point point: points) {
	// 				Pair<Integer, Point> move = new Pair(entry.getKey(), point);
	// 				System.out.println("Update: " + " x: " + point.x + " y: " + point.y);
	// 				if(check_validity(move, player_pieces, opponent_pieces)) {
	// 					System.out.println("Valid");
	// 					moves.add(move);
	// 					player_pieces.put(entry.getKey(), point);
	// 				} else {
	// 					System.out.println("Invalid");
	// 					break;
	// 				}
	// 			}
	// 			if (moves.size() >= 2) break;
	// 		}
	// 		return moves;
	// 	} else {
	// 		return null;
	// 	}
	// }

	private void initializeBlockadeList(boolean isplayer1) {
		for (double blockade_ycoord: BLOCKADE_YCOORD) {
			Point blockade = new Point(isplayer1 ? 19.99 : -19.99, blockade_ycoord);
			this.blockadeList.add(blockade);
		}
	}

	// Construct the blockade points and return a priority list of the blockade points we want to fill
	private List<Point> computeBlockadeImportance(Map<Integer, Point> opponent_pieces, boolean isplayer1) {
		// Can code this to assign importance to which part of the blockade is computed first
		// For now, this just constructs the blockade as the default list ordering
		return this.blockadeList;
	}

	private void removeOccupiedBlockadePoints(Map<Integer, Point> player_pieces) {
		Set<Point> blockade_filled = new HashSet<>();
		for (Point blockade: this.blockadeList) {
			for (Point piece: player_pieces.values()) {
				if (getDistance(blockade, piece) < EPSILON) {
					blockade_filled.add(blockade);
				}
			}
		}
		for (Point filled: blockade_filled) {
			this.blockadeList.remove(filled);
		}
	}

	private void computeBlockadeMap(HashMap<Integer, Point> player_pieces, boolean isplayer1) {
		this.blockadeMap = new HashMap<>();
		// This is just to create a deep copy of available pieces, to prevent re-using the same piece in the blockade
		Set<Integer> unused_pieces = player_pieces.keySet().stream().collect(Collectors.toSet());
		for (Point target: blockadeList) {
			double shortest_distance = -1;
			Integer best_piece = -1;
			for (Integer unused_piece: unused_pieces) {
				Point current = player_pieces.get(unused_piece);
				double distance = getDistance(current, target, 2);
				if (shortest_distance == -1 || distance < shortest_distance) {
					shortest_distance = distance;
					best_piece = unused_piece;
				}
			}
			this.blockadeMap.put(target, best_piece);
			unused_pieces.remove(best_piece);
		}
		for (Map.Entry<Point, Integer> entry: this.blockadeMap.entrySet()) {
			System.out.println("Point ID: " + entry.getValue() + " Current: " + player_pieces.get(entry.getValue()).x + " " + player_pieces.get(entry.getValue()).y + " Target: " + entry.getKey().x + " " + entry.getKey().y);
		}
	}

	// Given a current point and a target point, move the current point towards the current point
	// If this cannot be done in a single move, move the current point directly towards the target
	// The calling method will have to store the second move if it can only make one more move

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
		if (check_validity(next, player_pieces, opponent_pieces)) {
			moves.add(m1);
			Point m2 = getNewPointFromOldPointAndAngle(m1, phi);
			moves.add(m2);
		} else {
			m1 = getNewPointFromOldPointAndAngle(current_point, phi);
			if (check_validity(next, player_pieces, opponent_pieces)) {
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

	private double getDistance(Point p1, Point p2) {
		return getDistance(p1, p2, 1);
	}

	private double getDistance(Point p1, Point p2, int y_scale) {
		double x_diff = p2.x-p1.x;
		double y_diff = (p2.y-p1.y)/y_scale;
		return Math.sqrt(x_diff*x_diff + y_diff*y_diff);
	}

	private HashMap<Integer, Point> deepCopy(HashMap<Integer, Point> map) {
		HashMap<Integer, Point> copy = new HashMap<>();
		for (Map.Entry<Integer, Point> entry: map.entrySet()) {
			copy.put(new Integer(entry.getKey()), new Point(entry.getValue()));
		}
		return copy;
	}

	private double evaluateSquare(HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces) {
		double value = 0.0;
		for (Map.Entry<Integer, Point> entry: player_pieces.entrySet()) {
			value += (60+entry.getValue().x) * (60+entry.getValue().x);
		}
		return value;
	}

	private double evaluateV(HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, Point target) {
		double value = 0.0;
		for (Map.Entry<Integer, Point> entry: player_pieces.entrySet()) {
			double x_diffsq = (entry.getValue().x - target.x)*(entry.getValue().x - target.x);
			double y_diffsq = (entry.getValue().y - target.y)*(entry.getValue().y - target.y);
			value += Math.sqrt(x_diffsq + y_diffsq);
		}
		return value;
	}

	// Utility Methods

	// Given your piece and all other pieces, return the farthest forward point (can get stuck locally)
	public Point getBestNonConflictingLocalMove(HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, Integer piece) {
		double theta = -Math.PI/2;
		double delta_x = diameter_piece * Math.cos(theta);
		double delta_y = diameter_piece * Math.sin(theta);
		return new Point(0.0, 0.0);
	}

	// Given your piece and all other pieces, return the next move on the shortest path leading to the end (should not get stuck locally)
	public Point getBestNonConflictingGlobalMove(HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, Integer piece) {
		return new Point(0.0, 0.0);
	}


	public boolean check_validity(Pair<Integer, Point> move, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces)
    {
        boolean valid = true;
       
        // check if move is adjacent to previous position.
        if(!Board.almostEqual(Board.getdist(player_pieces.get(move.getKey()), move.getValue()), diameter_piece))
            {
                return false;
            }
        // check for collisions
        valid = valid && !Board.check_collision(player_pieces, move);
        valid = valid && !Board.check_collision(opponent_pieces, move);

        // check within bounds
        valid = valid && Board.check_within_bounds(move);
        return valid;

    }
}

