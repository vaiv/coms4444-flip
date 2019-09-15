package flip.wall;

import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.HashMap;
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

	private boolean wall_init = false;
	// temporary variable to store current target position of each piece
	private HashMap<Integer, Point> piece_to_dest = new HashMap<Integer, Point>();
	// points selected to be the wall
	private HashMap<Integer, Point> wall_point = new HashMap<Integer, Point>();

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

		// Set current wall buidling objective
		if (!wall_init) {
			ArrayList<Point> wall = wall_points(isplayer1 ? 18.0 : -18.0, 1.0); // positions for wall
			for (Point point : wall) {
				Integer id = closest(point, player_pieces, wall_point);
				piece_to_dest.put(id, point);
				wall_point.put(id, point);
			}
			wall_init = true;
		}

		for (Integer id : piece_to_dest.keySet()) {
			if (moves.size() >= num_moves || i >= num_trials) 
				break;
			Point dest = piece_to_dest.get(id);
			Point temp = find_move(player_pieces.get(id), dest);
			Pair<Integer, Point> move = new Pair<Integer, Point>(id, temp);
			if(check_validity(move, player_pieces, opponent_pieces))
				moves.add(move);
			++i;	 
		}

		return moves;

		// while(moves.size()!= num_moves && i<num_trials)
		// {
		//  	Integer piece_id = random.nextInt(n);
		//  	Point curr_position = player_pieces.get(piece_id);
		// 	Point new_position = new Point(curr_position);
			

			
		// 	Point temp = find_move(curr_position, new Point(0, 0));

		//  	Pair<Integer, Point> move = new Pair<Integer, Point>(piece_id, temp);

		//  	Double dist = Board.getdist(player_pieces.get(move.getKey()), move.getValue());
		//  	// System.out.println("distance from previous position is " + dist.toString());
		//  	// Log.record("distance from previous position is " + dist.toString());

		//  	if(check_validity(move, player_pieces, opponent_pieces))
		//  		moves.add(move);
		//  	i++;
		// }
		 
		// return moves;
	}

	// Find the distance between two points
	public double distance(Point x, Point y) {
		double y_diff = x.y - y.y;
		double x_diff = x.x - y.x;
		double dist = Math.sqrt(Math.pow(x_diff, 2) + Math.pow(y_diff, 2));
		return dist;
	}

	// Find a valid move that moves a coin closer to destination
	// TODO: more advanced path finding algorithm
	public Point find_move(Point start, Point dest) {
		double length = this.diameter_piece;
		double y_diff = dest.y - start.y;
		double x_diff = dest.x - start.x;
		if (Math.sqrt(Math.pow(x_diff, 2) + Math.pow(y_diff, 2)) <= length) {
			return new Point(dest);
		}
		double ratio = length / Math.sqrt(Math.pow(x_diff, 2) + Math.pow(y_diff, 2));
		double newX = start.x + x_diff * ratio;
		double newY = start.y + y_diff * ratio;
		return new Point(newX, newY);
	}

	// Find the id of a coin that's closest to a point, ignoring some
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
	
	// Find a list of points at x axis that can form a vertical wall
	// spacing is the space between each coin
	public ArrayList<Point> wall_points(double x, double spacing) {
		ArrayList<Point> result = new ArrayList<Point>();
		double d = this.diameter_piece;
		double y_max = 20.0;
		double y_min = -20.0;
		double curr_y = y_min + d/2;
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

