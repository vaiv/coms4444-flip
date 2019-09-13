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

	private static final double EPSILON = 0.01;
	private static final double B1  = -17.30;
	private static final double B2  = -13.84;
	private static final double B3  = -10.38;
	private static final double B4  = -6.92;
	private static final double B5  = -3.46;
	private static final double B6  = 0.00;
	private static final double B7  = 3.46;
	private static final double B8  = 6.92;
	private static final double B9  = 10.38;
	private static final double B10 = 13.84;
	private static final double B11 = 17.30;

	private static final Set<Double> BLOCKADE_SET = new HashSet<>(Arrays.asList(B1, B2, B3, B4, B5, B6, B7, B8, B9, B10, B11));
	private Map<Integer, Point> blockadeMap = new HashMap<>();

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
		computeBlockadeMap(pieces, isplayer1);
	}

	public List<Pair<Integer, Point>> getMoves(Integer num_moves, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, boolean isplayer1)
	{

		return getMovesForBlockade(player_pieces, opponent_pieces, isplayer1);

		// System.out.println("moves requested");
		// List<Pair<Integer, Point>> moves = new ArrayList<Pair<Integer, Point>>();
		 
		// Pair<Integer, Point> best_move = getBestMove(player_pieces, opponent_pieces, isplayer1);
		// moves.add(best_move);
		// player_pieces.put(best_move.getKey(), best_move.getValue());
		// Pair<Integer, Point> best_move2 = getBestMove(player_pieces, opponent_pieces, isplayer1);
		// moves.add(best_move2);
		 
		// return moves;
	}

	private List<Pair<Integer, Point>> getMovesForBlockade(HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, boolean isplayer1) {
		if (n >= 11) {
			List<Pair<Integer, Point>> moves = new ArrayList<>();
			Set<Integer> finished = new HashSet<>();
			for (Map.Entry<Integer, Point> entry: blockadeMap.entrySet()) {
				Point current = player_pieces.get(entry.getKey());
				Point target = entry.getValue();
				double d = getDistance(current, target, 1);
				if (d < EPSILON) {
					finished.add(entry.getKey());
				}
			}
			for (Integer i: finished) {
				blockadeMap.remove(i);
			}

			for (Map.Entry<Integer, Point> entry: blockadeMap.entrySet()) {
				System.out.println("Start: " + " x: " + player_pieces.get(entry.getKey()).x +
					" y: " + player_pieces.get(entry.getKey()).y);
				System.out.println("Target: " + " x: " + entry.getValue().x +
					" y: " + entry.getValue().y);
				List<Point> points = moveCurrentToTarget(player_pieces.get(entry.getKey()), entry.getValue());
				for (Point point: points) {
					Pair<Integer, Point> move = new Pair(entry.getKey(), point);
					System.out.println("Update: " + " x: " + point.x + " y: " + point.y);
					if(check_validity(move, player_pieces, opponent_pieces)) {
						System.out.println("Valid");
						moves.add(move);
						player_pieces.put(entry.getKey(), point);
					} else {
						System.out.println("Invalid");
						break;
					}
				}
				if (moves.size() >= 2) break;
			}
			return moves;
		} else {
			return null;
		}
	}

	// Pick each blockade point and find the optimal piece to use
	// This is a greedy algorithm that could be improved if necessary
	private void computeBlockadeMap(HashMap<Integer, Point> player_pieces, boolean isplayer1) {
		blockadeMap = new HashMap<>();
		// This is just to create a deep copy of available pieces, to prevent re-using the same piece in the blockade
		Set<Integer> unused_pieces = player_pieces.keySet().stream().collect(Collectors.toSet());
		for (double target_y: BLOCKADE_SET) {
			Point target = new Point(isplayer1 ? 19.99 : -19.99, target_y);
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
			blockadeMap.put(best_piece, target);
			unused_pieces.remove(best_piece);
		}
		for (Map.Entry<Integer, Point> entry: blockadeMap.entrySet()) {
			System.out.println("Point ID: " + entry.getKey() + " Current: " + player_pieces.get(entry.getKey()).x + " " + player_pieces.get(entry.getKey()).y + " Target: " + entry.getValue().x + " " + entry.getValue().y);
		}
	}

	// Given a current point and a target point, move the current point towards the current point
	// If this cannot be done in a single move, move the current point directly towards the target
	// The calling method will have to store the second move if it can only make one more move

	public List<Point> moveCurrentToTarget(Point current, Point target) {
		List<Point> moves = new ArrayList<>();
		double tmcx = target.x-current.x;
		double tmcy = target.y-current.y;
		double d = Math.sqrt(tmcx*tmcx + tmcy*tmcy);
		double theta = Math.atan(tmcy/tmcx);
		// if (d > 3*diameter_piece) {
			if (d >= 0.01 && d < 2*diameter_piece) {
				moves.addAll(moveCurrentToTargetClose(current, target));
			// } else if (d >= 2*diameter_piece && d < 3*diameter_piece) {
			// 	Point new_position = getNewPointFromOldPointAndAngle(current, theta);
			// 	moves.add(new_position);
			// 	moves.addAll(moveCurrentToTargetClose(new_position, target));
			} else {
				Point m1 = getNewPointFromOldPointAndAngle(current, theta);
				moves.add(m1);
				Point m2 = getNewPointFromOldPointAndAngle(m1, theta);
				moves.add(m2);	
			}
		// }
		return moves;
	}

	public List<Point> moveCurrentToTargetClose(Point current, Point target) {
		List<Point> moves = new ArrayList<>();
		double tmcx = target.x-current.x;
		double tmcy = target.y-current.y;
		// We need to solve for a 2-move sequence that gets the current point to the target
		double tmcx2 = tmcx/2;
		double tmcy2 = tmcy/2;
		// tpp2 is (theta + phi)/2
		double tpp2 = Math.atan(tmcy/tmcx);
		// tmp2 is (theta - phi)/2
		double tmp2 = Math.acos(Math.sqrt(tmcx2*tmcx2 + tmcy2*tmcy2)/2);
		double theta = tpp2 + tmp2;
		double phi = tpp2 - tmp2;
		// Note - if you are blocked, maybe you can take the other angle first!?
		Point m1 = getNewPointFromOldPointAndAngle(current, theta);
		moves.add(m1);
		Point m2 = getNewPointFromOldPointAndAngle(m1, phi);
		moves.add(m2);
		return moves;
	}

	public Point getNewPointFromOldPointAndAngle(Point current, double theta) {
		Point new_position = new Point(current);
		double delta_x = diameter_piece * Math.cos(theta);
		double delta_y = diameter_piece * Math.sin(theta);
		new_position.x += isplayer1 ? -delta_x : delta_x;
		new_position.y -= delta_y;
		return new_position;
	}

	private double getDistance(Point p1, Point p2, int y_scale) {
		double x_diff = p2.x-p1.x;
		double y_diff = (p2.y-p1.y)/y_scale;
		return Math.sqrt(x_diff*x_diff + y_diff*y_diff);
	}

	private Pair<Integer, Point> getBestMove(HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, boolean isplayer1) {
		HashMap<Integer, Point> player_pieces_eval = deepCopy(player_pieces);
		Pair<Integer, Point> best_move = null;
		Pair<Integer, Point> move = null;
		Point target = isplayer1 ? new Point(-30.0, 0.0) : new Point(30.0, 0.0);
		Double best_value = evaluateV(player_pieces, opponent_pieces, target);
		System.out.println("current_value: " + best_value);
		for (int i = 0; i < n; i++) { // iterate through keys instead
		 	Point curr_position = player_pieces.get(i);
		 	Point new_position = new Point(curr_position.x, curr_position.y);
		 	new_position.x = isplayer1 ? new_position.x - 2 : new_position.x + 2;
		 	new_position.y = new_position.y;
		 	move = new Pair<Integer, Point>(i, new_position);
		 	if(check_validity(move, player_pieces, opponent_pieces)) {
		 		player_pieces_eval.put(move.getKey(), move.getValue());
		 		Double value = evaluateV(player_pieces_eval, opponent_pieces, target);
		 		System.out.println("valid move! move: " + move + " value: " + value);
		 		if (value < best_value) {
		 			best_value = value;
		 			best_move = move;
		 		}
		 		// if (isplayer1 && (value < best_value)) {
		 		// 	best_value = value;
		 		// 	best_move = move;
		 		// } else if (!isplayer1 && (value > best_value)) {
		 		// 	best_value = value;
		 		// 	best_move = move;
		 		// }
		 	}
			player_pieces_eval = deepCopy(player_pieces); // better way to reset by undoing what was done
		}
		System.out.println("best move! move: " + best_move + " best_value: " + best_value);
		return best_move;
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

