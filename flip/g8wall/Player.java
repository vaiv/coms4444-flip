package flip.g8wall;

import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.HashMap;
import java.util.HashSet;

import javafx.util.Pair; 
import java.util.ArrayList;

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

	// Stage variable: if the wall has been initialized
	private boolean wall_init = false;
	// Stage variable: if the wall has finished
	private boolean wall_complete = false;
	// points that's used to build a wall
	private HashMap<Integer, Point> wall_point = new HashMap<Integer, Point>();
	// indicates if a coin should be not be moved, like coins forming a wall
	private HashSet<Integer> ignored_piece = new HashSet<Integer>();
	// indexes for attackers
	private HashSet<Integer> attackers = new HashSet<Integer>();
	// temporary variable to store current destination of each piece
	private ArrayList<Pair<Integer, Point>> piece_to_dest = new ArrayList<Pair<Integer, Point>>();
	// cached moves, the moves is always valid
	private ArrayList<Pair<Integer, Point>> cached_moves = new ArrayList<Pair<Integer, Point>>();

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
	}

	public List<Pair<Integer, Point>> getMoves(Integer num_moves, HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces, boolean isplayer1)
	{
		List<Pair<Integer, Point>> moves = new ArrayList<Pair<Integer, Point>>();

		int num_trials = 30;
		int i = 0;

		// TODO: if n < 20, use greedy

		// Set current wall buidling objective
		if (!wall_init) {
			ArrayList<Point> wall = find_wall_points(isplayer1 ? 21 : -21, 1.45, 1.75); // positions for wall
			for (Point point : wall) {
				Integer id = closest(point, player_pieces, wall_point);
				piece_to_dest.add(new Pair<Integer, Point>(id, point));
				wall_point.put(id, point);
				//System.out.println("point" + point.x + "," + point.y);
			}
			wall_init = true;
		}

		// for (Pair<Integer, Point> pair : piece_to_dest) {
		// 	Integer id = pair.getKey();
		// 	Point dest = pair.getValue();
		// 	if (ignored_piece.contains(id)) continue;
		// 	while (moves.size() < num_moves && i < num_trials) {
		// 		Point temp = find_move_exact(player_pieces.get(id), dest);
		// 		Pair<Integer, Point> move = new Pair<Integer, Point>(id, temp);
		// 		if(check_validity(move, player_pieces, opponent_pieces))
		// 			moves.add(move);
		// 		++i;
		// 	}
		// }

		for (Pair<Integer, Point> pair : piece_to_dest) { // TODO: adaptive wall building
			if (ignored_piece.size() == wall_point.size()) wall_complete = true;
			if (wall_complete) break;

			// adaptive wall building, priorize walls that blocks enemy runners
			// Point enemy_runner_pos = opponent_pieces.get(find_runner(opponent_pieces, isplayer1));
			// Integer adapive_wall = piece_to_dest.get(0).getKey();
			// double min_wall_dist = Math.abs(piece_to_dest.get(0).getValue().y - enemy_runner_pos.y);
			// for (Pair<Integer, Point> p : piece_to_dest) {
			// 	if (Math.abs(p.getValue().y - enemy_runner_pos.y) < min_wall_dist){
			// 		min_wall_dist = Math.abs(p.getValue().y - enemy_runner_pos.y);
			// 		adapive_wall = p.getKey();
			// 	}
			// }

			// System.out.println(min_wall_dist);
			// if (!ignored_piece.contains(adapive_wall)) 
			// 	pair = new Pair<Integer, Point>(adapive_wall, wall_point.get(adapive_wall));

			Integer id = pair.getKey();
			Point dest = pair.getValue(); 

			// mark the coins already in wall position, skip those already in position
			if (ignored_piece.contains(id)) continue;
			if (Board.almostEqual(distance(player_pieces.get(id), dest), 0.0)) {
				ignored_piece.add(id);
				continue;
			}

			if (moves.size() >= num_moves || i >= num_trials) 
				break;
			
			Point temp = find_move_exact(player_pieces.get(id), dest, 1); // find the next move
			Pair<Integer, Point> move = new Pair<Integer, Point>(id, temp);
			if(check_validity(move, player_pieces, opponent_pieces))
				moves.add(move);
			else { // Try move the coin in other direction
				temp = find_move_exact(player_pieces.get(id), dest, -1);
				move = new Pair<Integer, Point>(id, temp);
				if (check_validity(move, player_pieces, opponent_pieces)) 
					moves.add(move);
				else { // move backwards to make space
					// TODO: random
					temp = player_pieces.get(id);
					temp.x -= isplayer1 ? -diameter_piece : diameter_piece;
					move = new Pair<Integer, Point>(id, temp);
				}
			}
			++i;	 
		}

		if (ignored_piece.size() == wall_point.size()) wall_complete = true;

		// start attacking
		// TODO: attack
		if (wall_complete) {
			int wall_size = wall_point.size();
			ArrayList<Integer> wall_point_index = new ArrayList<Integer>(wall_point.keySet());
			ArrayList<Point> wall_point_array = new ArrayList<Point>(wall_point.values());
			wall_point_array.sort((Point p1, Point p2) -> (p1.y > p2.y) ? 1 : -1);

			Point attack_pos = new Point(wall_point_array.get(wall_size - 1)); // position to start the attack
			Integer wall_index = closest(attack_pos, player_pieces, (HashSet<Integer>)null); // wall next to the attacker
			attack_pos.x -= isplayer1 ? -diameter_piece : diameter_piece;
			Integer attacker = closest(attack_pos, player_pieces, ignored_piece);
			while (moves.size() < num_moves && i < num_trials) {
				Point temp = find_move_exact(player_pieces.get(attacker), attack_pos, 1);
				Pair<Integer, Point> move = new Pair<Integer, Point>(attacker, temp);
				if(check_validity(move, player_pieces, opponent_pieces)) {
					moves.add(move);
					temp = find_move_exact(temp, attack_pos, 1);
					move = new Pair<Integer, Point>(attacker, temp);
					moves.add(move);
				}
				else {
					temp = find_move_exact(player_pieces.get(attacker), attack_pos, -1);
					move = new Pair<Integer, Point>(attacker, temp);
					if (check_validity(move, player_pieces, opponent_pieces))
						moves.add(move);
					else {
						// TODO: random
					}
				}
				++i;
			}
		}
		return moves;
	}

	// Find the index of the coin with largest/smallest x-axis (for player1 is largest)
	public Integer find_runner(HashMap<Integer, Point> pieces, boolean isplayer1) {
		Integer index = 0;
		double x_axis = pieces.get(index).x;
		for (int i : pieces.keySet()) {
			if (isplayer1) {
				if (pieces.get(i).x > x_axis) {
					x_axis = pieces.get(i).x;
					index = i;
				}
			}
			else {
				if (pieces.get(i).x < x_axis) {
					x_axis = pieces.get(i).x;
					index = i;
				}
			}
		}
		return index;
	}

	// Find the distance between two points
	public double distance(Point x, Point y) {
		double y_diff = x.y - y.y;
		double x_diff = x.x - y.x;
		double dist = Math.sqrt(Math.pow(x_diff, 2) + Math.pow(y_diff, 2));
		return dist;
	}

	// Find a valid move that moves a coin exactly to the destination
	// when there are two possible moves, set choice to -1 will give different result, choice default to 1 
	// TODO: more advanced path finding algorithm, avoid obstacles, refractor
	// WARNING: this function one only works when start and dest are both on the left or right side
	public Point find_move_exact(Point start, Point dest, int choice) {
		if (isplayer1) { // Very questionable logic here
			start = new Point(start);
			dest = new Point(dest);
			start.x *= -1;
			dest.x *= -1;
		}
		Point result;
		double length = this.diameter_piece;
		double y_diff = dest.y - start.y;
		double x_diff = dest.x - start.x;
		double dist = Math.sqrt(Math.pow(x_diff, 2) + Math.pow(y_diff, 2));
		if (Board.almostEqual(dist, length)) {
			result = new Point(dest);
		}
		else if (dist <= 2 * length) { 
			double newX = start.x;
			double newY = start.y;
			double vector1_x, vector1_y, vector2_x, vector2_y;
			vector1_x = 0.5 * (dest.x - start.x);
			vector1_y = 0.5 * (dest.y - start.y);
			double vector1_length = dist / 2;
			double vector2_length = Math.sqrt(Math.pow(length, 2) - Math.pow(vector1_length, 2));

			vector2_x = choice*(-(vector1_y / vector1_length) * vector2_length);
			vector2_y = choice*((vector1_x / vector1_length) * vector2_length);

			newX += (vector1_x + vector2_x);
			newY += (vector1_y + vector2_y);
			result = new Point(newX, newY);
		}
		else {
			double ratio = length / dist;
			double newX = start.x + x_diff * ratio;
			double newY = start.y + y_diff * ratio;
			result = new Point(newX, newY);
		}
		if (isplayer1) { // Very questionable logic here :(
			result.x *= -1;
		}
		return result;
	}

	// Find two moves that moves a coin to the destination
	// public ArrayList<Pair<Integer, Point>> find_two_moves(Point start, Point dest, Integer index, 
	// 	   HashMap<Integer, Point> player_pieces, HashMap<Integer, Point> opponent_pieces) {
	// 	ArrayList<Pair<Integer, Point>> moves = new ArrayList<Pair<Integer, Point>>();
	// 	Point temp = find_move_exact(start, dest, 1);
	// 	Pair<Integer, Point> move = new Pair<Integer, Point>(index, temp);
	// 	if(check_validity(move, player_pieces, opponent_pieces)) {
	// 		moves.add(move);
	// 		temp = find_move_exact(temp, dest, 1);
	// 		move = new Pair<Integer, Point>(attacker, temp);
	// 			}
	// 			else {
	// 				temp = find_move_exact(player_pieces.get(attacker), attack_pos, -1);
	// 				move = new Pair<Integer, Point>(attacker, temp);
	// 				if (check_validity(move, player_pieces, opponent_pieces))
	// 					moves.add(move);
	// 				else {
	// 					// TODO: random
	// 				}
	// 			}
	// }

	// Find the id of a coin that's closest to a point, ignoring some visited
	public Integer closest(Point point, HashMap<Integer, Point> pieces, HashMap<Integer, Point> ignore) {
		double min_distance = Double.MAX_VALUE;
		Integer result_id = 0;
		for (int i = 0; i < n; ++i) {
			if (ignore != null && ignore.containsKey(i)) 
				continue;
			Integer id = i;
			Point p = pieces.get(id);
			double distance = Math.sqrt(Math.pow(point.x - p.x, 2) + Math.pow(point.y - p.y, 2));
			if (distance < min_distance) {
				min_distance = distance;
				result_id = id;
			} 
		}
		return result_id;
	}

	public Integer closest(Point point, HashMap<Integer, Point> pieces, HashSet<Integer> ignore) {
		double min_distance = Double.MAX_VALUE;
		Integer result_id = 0;
		for (int i = 0; i < n; ++i) {
			if (ignore != null && ignore.contains(i)) 
				continue;
			Integer id = i;
			Point p = pieces.get(id);
			double distance = Math.sqrt(Math.pow(point.x - p.x, 2) + Math.pow(point.y - p.y, 2));
			if (distance < min_distance) {
				min_distance = distance;
				result_id = id;
			} 
		}
		return result_id;
	}
	
	// Find a list of points at x axis that can form a vertical wall
	// spacing is the space between each coin
	public ArrayList<Point> find_wall_points(double x, double spacing, double start) {
		ArrayList<Point> result = new ArrayList<Point>();
		double d = this.diameter_piece;
		double y_max = 20.0;
		double y_min = -20.0;
		double curr_y = y_min + d/2 + start;
		for (;curr_y + d/2 <= y_max; curr_y += d + spacing) {
			result.add(new Point(x, curr_y));
		}
		return result;
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

